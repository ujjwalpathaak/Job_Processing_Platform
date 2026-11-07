-- AlterTable
ALTER TABLE "rag_embeddings" ADD COLUMN IF NOT EXISTS "job_id" BIGINT;
ALTER TABLE "rag_embeddings" ADD COLUMN IF NOT EXISTS "level" VARCHAR(16);
ALTER TABLE "rag_embeddings" ADD COLUMN IF NOT EXISTS "handler" VARCHAR(64);
ALTER TABLE "rag_embeddings" ADD COLUMN IF NOT EXISTS "embedding_model" VARCHAR(64);

-- CreateIndex
CREATE INDEX IF NOT EXISTS "idx_rag_job_id" ON "rag_embeddings"("job_id");
CREATE INDEX IF NOT EXISTS "idx_rag_created_at" ON "rag_embeddings"("created_at");
