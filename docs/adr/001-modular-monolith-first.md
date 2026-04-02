# ADR-001: Start with Modular Monolith Architecture

## Status
Accepted

## Context
We need to build an enterprise AI platform that supports document ingestion, RAG-based chat, and multi-agent orchestration. We must decide between starting with microservices or a monolith.

Key considerations:
- Team size: Small initial team
- Domain understanding: Still evolving
- Deployment complexity: Need to minimize operational overhead initially
- Future scale: Unknown load patterns
- Development velocity: Need to ship features quickly

## Decision
We will implement EKOC as a **modular monolith** with clear module boundaries.

## Rationale

### Why Modular Monolith?

1. **Faster Development**
   - Single deployment unit
   - No distributed system complexity
   - Simplified debugging and testing
   - Direct method calls (no network overhead)

2. **Bounded Contexts Still Defined**
   - Modules: auth, users, teams, documents, ingestion, retrieval, chat, memory, audit, admin
   - Each module has clear package structure
   - Interfaces between modules well-defined
   - Can extract to microservices later

3. **Operational Simplicity**
   - Single database (easier transactions)
   - One deployment pipeline
   - Simpler monitoring
   - Lower infrastructure cost

4. **Domain Discovery**
   - Don't know optimal service boundaries yet
   - Easier to refactor within monolith
   - Can identify true boundaries through usage

### Design for Future Migration

We ensure microservices readiness by:
- **No cross-module entity references**: Modules reference each other by ID only
- **Event-driven where appropriate**: RabbitMQ for async operations
- **API-first**: All modules expose service interfaces
- **Database per module pattern**: Logical schema separation
- **Stateless design**: JWT tokens, no session affinity

### When to Migrate

Extract modules to microservices when:
- Specific module needs independent scaling (likely: ingestion, retrieval)
- Team grows beyond 8-10 developers
- Different modules have conflicting technology needs
- Deployment independence becomes critical
- Performance profiling shows clear bottlenecks

## Consequences

### Positive
- Faster initial development
- Simpler local development setup
- Easier end-to-end testing
- Lower operational complexity
- Clear upgrade path to microservices

### Negative
- All modules must use same tech stack (Java/Spring)
- Cannot independently scale modules (yet)
- Single deployment means all-or-nothing releases
- Database connection pool shared across modules

### Mitigations
- Strict module boundaries prevent tight coupling
- Feature flags enable partial rollouts
- RabbitMQ allows async processing isolation
- Monitoring per-module performance metrics

## References
- [Modular Monoliths](https://www.kamilgrzybek.com/design/modular-monolith-primer/)
- [Monolith First](https://martinfowler.com/bliki/MonolithFirst.html)
