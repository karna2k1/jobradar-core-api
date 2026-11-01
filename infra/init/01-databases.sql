CREATE DATABASE jobradar_core;
CREATE DATABASE jobradar_ingest;
CREATE DATABASE jobradar_match;
\connect jobradar_match
CREATE EXTENSION IF NOT EXISTS vector;
