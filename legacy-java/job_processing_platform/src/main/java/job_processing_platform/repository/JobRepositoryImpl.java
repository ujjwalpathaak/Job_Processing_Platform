package job_processing_platform.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import job_processing_platform.dto.JobQueryOptionsDTO;
import job_processing_platform.entity.Job;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import job_processing_platform.enums.JobStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JobRepositoryImpl implements JobRepositoryCustom {

    private static final Map<String, String> SORT_BY_FIELD = Map.of(
            "createdAt", "j.createdAt",
            "updatedAt", "j.updatedAt",
            "status", "j.status",
            "jobHandler", "j.jobHandler",
            "jobCategory", "j.jobCategory"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Job> findWithOptions(JobQueryOptionsDTO options, Instant since) {
        BuildResult build = buildConditions(options, since);
        if (build.emptyResult()) {
            return List.of();
        }

        String defaultSort = (since != null) ? "j.updatedAt" : "j.createdAt";
        String sortField = (options != null && options.sortBy() != null)
                ? SORT_BY_FIELD.getOrDefault(options.sortBy(), defaultSort)
                : defaultSort;
        String sortOrder = (options != null && "asc".equalsIgnoreCase(options.sortOrder())) ? "ASC" : "DESC";

        String jpql = "SELECT j FROM Job j" + build.whereClause() + " ORDER BY " + sortField + " " + sortOrder;
        TypedQuery<Job> query = entityManager.createQuery(jpql, Job.class);
        build.params().forEach(query::setParameter);

        if (options != null && options.limit() != null) {
            query.setMaxResults(options.limit());
        }
        if (options != null && options.offset() != null) {
            query.setFirstResult(options.offset());
        }

        return query.getResultList();
    }

    @Override
    public long countWithOptions(JobQueryOptionsDTO options, Instant since) {
        BuildResult build = buildConditions(options, since);
        if (build.emptyResult()) {
            return 0L;
        }

        String jpql = "SELECT COUNT(j) FROM Job j" + build.whereClause();
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        build.params().forEach(query::setParameter);
        Long result = query.getSingleResult();
        return result != null ? result : 0L;
    }

    private BuildResult buildConditions(JobQueryOptionsDTO options, Instant since) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();

        if (since != null) {
            conditions.add("j.updatedAt > :since");
            params.put("since", since);
        }

        if (options != null) {
            if (options.handler() != null) {
                try {
                    JobHandlerType type = JobHandlerType.valueOf(options.handler().toUpperCase());
                    conditions.add("j.jobHandler = :handler");
                    params.put("handler", type);
                } catch (IllegalArgumentException ignored) {
                    return BuildResult.empty();
                }
            }

            if (options.status() != null) {
                try {
                    JobStatus status = JobStatus.valueOf(options.status().toUpperCase());
                    conditions.add("j.status = :status");
                    params.put("status", status);
                } catch (IllegalArgumentException ignored) {
                    return BuildResult.empty();
                }
            }

            if (options.category() != null) {
                try {
                    JobCategory category = JobCategory.valueOf(options.category().toUpperCase());
                    conditions.add("j.jobCategory = :category");
                    params.put("category", category);
                } catch (IllegalArgumentException ignored) {
                    return BuildResult.empty();
                }
            }

            if (options.search() != null && !options.search().isBlank()) {
                String searchParam = "%" + options.search().toLowerCase() + "%";
                conditions.add(
                        "(LOWER(CAST(j.id AS String)) LIKE :search" +
                        " OR LOWER(CAST(j.jobHandler AS String)) LIKE :search" +
                        " OR LOWER(CAST(j.jobCategory AS String)) LIKE :search" +
                        " OR LOWER(CAST(j.status AS String)) LIKE :search)"
                );
                params.put("search", searchParam);
            }
        }

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new BuildResult(false, whereClause, params);
    }

    private record BuildResult(boolean emptyResult, String whereClause, Map<String, Object> params) {
        static BuildResult empty() {
            return new BuildResult(true, "", Map.of());
        }
    }
}
