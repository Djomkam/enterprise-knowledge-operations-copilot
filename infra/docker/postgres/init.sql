-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Grant permissions to ekoc user
GRANT ALL PRIVILEGES ON DATABASE ekoc TO ekoc;

-- Create keycloak database for Keycloak's internal storage
CREATE DATABASE keycloak OWNER ekoc;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO ekoc;
