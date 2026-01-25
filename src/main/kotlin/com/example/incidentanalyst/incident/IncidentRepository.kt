package com.example.incidentanalyst.incident

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class IncidentRepository : PanacheRepository<IncidentEntity> {

    fun findRecent(limit: Int): List<IncidentEntity> =
        find("order by createdAt desc").page(0, limit).list()
}
