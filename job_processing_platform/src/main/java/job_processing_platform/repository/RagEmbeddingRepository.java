package job_processing_platform.repository;

import job_processing_platform.entity.RagEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RagEmbeddingRepository extends JpaRepository<RagEmbedding, Long> {

    @Query(value = """
            SELECT *
            FROM rag_embeddings
            ORDER BY embedding <-> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<RagEmbedding> findNearest(@Param("embedding") String embedding, @Param("limit") int limit);
}
