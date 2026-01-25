package com.example.incidentanalyst.rag

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RunbookEmbeddingRepository : PanacheRepository<RunbookEmbeddingEntity>
