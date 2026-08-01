-- Runs automatically on first container start (docker-entrypoint-initdb.d).
-- Enables the pgvector extension so we can use the `vector` column type
-- and the <=> cosine-distance operator in native queries later.
CREATE EXTENSION IF NOT EXISTS vector;