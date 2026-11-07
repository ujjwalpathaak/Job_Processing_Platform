package job_processing_platform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "rag_embeddings")
public class RagEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private String sourceName;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private String requestId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(columnDefinition = "vector(1536)")
    private String embedding;

    public RagEmbedding() {
    }

    public RagEmbedding(String sourceType, String sourceName, String content, String requestId, String embedding) {
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.content = content;
        this.requestId = requestId;
        this.createdAt = Instant.now();
        this.embedding = embedding;
    }

    public Long getId() {
        return id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getContent() {
        return content;
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getEmbedding() {
        return embedding;
    }
}
