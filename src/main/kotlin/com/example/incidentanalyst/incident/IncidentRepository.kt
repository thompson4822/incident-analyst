package com.example.incidentanalyst.incident

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class IncidentRepository : PanacheRepository<IncidentEntity> {

    fun findRecent(limit: Int): List<IncidentEntity> =
        find("order by createdAt desc").page(0, limit).list()

    fun findByFilters(
        query: String?,
        status: String?,
        severity: String?,
        source: String?,
        limit: Int = 50
    ): List<IncidentEntity> {
        val params = mutableMapOf<String, Any>()
        val conditions = mutableListOf<String>()

        if (!query.isNullOrBlank()) {
            conditions.add("(lower(title) like :query or lower(description) like :query)")
            params["query"] = "%${query.lowercase()}%"
        }

        if (!status.isNullOrBlank() && status != "All") {
            conditions.add("upper(status) = :status")
            params["status"] = status.uppercase()
        }

        if (!severity.isNullOrBlank() && severity != "Severity") {
            conditions.add("upper(severity) = :severity")
            params["severity"] = severity.uppercase()
        }

        if (!source.isNullOrBlank() && source != "Source") {
            conditions.add("upper(source) = :source")
            params["source"] = source.uppercase()
        }

        val queryStr = if (conditions.isEmpty()) {
            "order by createdAt desc"
        } else {
            conditions.joinToString(" and ") + " order by createdAt desc"
        }

        return find(queryStr, params).page(0, limit).list()
    }
}
