# ADR-002: PostgreSQL + pgvector for Vector Storage

## Status
Accepted

## Context
We need to store document embeddings and perform similarity search for RAG. Options considered:
- Dedicated vector databases (Pinecone, Weaviate, Qdrant)
- Specialized vector stores (Milvus, ChromaDB)
- PostgreSQL + pgvector extension
- Embedding within existing database

Key requirements:
- Vector similarity search (cosine similarity)
- ACID transactions (coordinate metadata + vectors)
- Team-based access control (join with user/team tables)
- Production-ready reliability
- Operational simplicity

## Decision
We will use **PostgreSQL with the pgvector extension** for vector storage and similarity search.

## Rationale

### Why PostgreSQL + pgvector?

1. **Single Database**
   - Document metadata and vectors in same DB
   - ACID transactions across both
   - No eventual consistency issues
   - Simplified backup/restore

2. **Relational + Vector Queries**
   - Join vectors with document metadata
   - Filter by team_id, owner_id in same query
   - Complex access control logic possible
   - Aggregations and analytics on metadata

3. **Production Readiness**
   - PostgreSQL is battle-tested
   - Well-understood operations
   - Existing team expertise
   - Mature tooling (pg_dump, replication)

4. **pgvector Capabilities**
   - Supports cosine distance, L2 distance, inner product
   - IVFFlat indexing for fast approximate search
   - Good performance up to millions of vectors
   - Active development and community

5. **Operational Simplicity**
   - One database to backup
   - One connection pool
   - Familiar SQL operations
   - Standard PostgreSQL monitoring

### Performance Considerations

- **IVFFlat Index**: Approximate nearest neighbor search
  - Trade-off: Speed vs accuracy (acceptable for RAG)
  - Index parameters tunable (lists, probes)
- **Expected Scale**: Millions of chunks, thousands of concurrent users
- **Benchmark**: pgvector handles 1M+ vectors at <100ms latency

### When to Reconsider

Migrate to dedicated vector DB if:
- Vector count exceeds 10M+ (pgvector performance degrades)
- Need sub-10ms latency on complex queries
- Advanced vector operations required (e.g., multi-vector search)
- Team grows expertise in specialized vector DBs

## Consequences

### Positive
- Simplified architecture (one database)
- Strong consistency guarantees
- Familiar tooling and operations
- Easy access control with SQL joins
- Cost-effective (no additional DB service)

### Negative
- pgvector less mature than dedicated solutions
- Performance ceiling lower than specialized DBs
- Index tuning requires vector search expertise
- PostgreSQL resource usage increases (CPU for similarity)

### Mitigations
- Monitor query performance closely
- Tune IVFFlat parameters based on data size
- Consider read replicas for search workload
- Design schema for future migration (abstract vector ops)

## Implementation Notes

```sql
-- Enable extension
CREATE EXTENSION vector;

-- Create table with vector column
CREATE TABLE document_chunks (
  id UUID PRIMARY KEY,
  content TEXT,
  embedding vector(1536),  -- OpenAI embedding dimension
  ...
);

-- Create IVFFlat index
CREATE INDEX ON document_chunks
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Similarity search query
SELECT id, content, 1 - (embedding <=> query_vector) AS similarity
FROM document_chunks
WHERE team_id = ?
ORDER BY embedding <=> query_vector
LIMIT 5;
```

## References
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [pgvector Performance](https://jkatz05.com/post/postgres/pgvector-performance/)
- [Spring AI pgvector integration](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
