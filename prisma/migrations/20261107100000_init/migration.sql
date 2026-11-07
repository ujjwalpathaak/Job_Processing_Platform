CREATE EXTENSION IF NOT EXISTS vector;
-- CreateSchema
CREATE SCHEMA IF NOT EXISTS "public";

-- CreateEnum
CREATE TYPE "JobStatus" AS ENUM ('SCHEDULED', 'PUBLISHED', 'PROCESSING', 'PROCESSED', 'RETRY', 'ERROR', 'DEAD');

-- CreateEnum
CREATE TYPE "JobCategory" AS ENUM ('STANDARD', 'CRITICAL', 'EXTERNAL');

-- CreateEnum
CREATE TYPE "JobHandlerType" AS ENUM ('EMAIL', 'REPORT_GENERATION', 'NOTIFICATION_CLEANUP', 'WEBHOOK_TRIGGER', 'CRM_SYNC', 'PAYMENT', 'REFUND');

-- CreateTable
CREATE TABLE "jobs" (
    "id" BIGSERIAL NOT NULL,
    "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ(3) NOT NULL,
    "status" "JobStatus" NOT NULL,
    "jobCategory" "JobCategory" NOT NULL,
    "jobHandler" "JobHandlerType" NOT NULL,
    "error_message" TEXT,
    "data" JSONB NOT NULL,

    CONSTRAINT "jobs_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "job_state_history" (
    "id" BIGSERIAL NOT NULL,
    "job_id" BIGINT NOT NULL,
    "status" "JobStatus" NOT NULL,
    "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "error_message" TEXT,

    CONSTRAINT "job_state_history_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "rag_embeddings" (
    "id" BIGSERIAL NOT NULL,
    "source_type" TEXT NOT NULL,
    "source_name" TEXT NOT NULL,
    "content" VARCHAR(4000) NOT NULL,
    "request_id" TEXT,
    "createdAt" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "embedding" vector(768) NOT NULL,

    CONSTRAINT "rag_embeddings_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "idx_job_id" ON "job_state_history"("job_id");

-- CreateIndex
CREATE INDEX "idx_job_status" ON "job_state_history"("status");

