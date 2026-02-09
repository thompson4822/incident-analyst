package com.example.incidentanalyst.runbook

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RunbookFragmentRepository : PanacheRepository<RunbookFragmentEntity> {

    fun findRecent(limit: Int = 50): List<RunbookFragmentEntity> =
        find("order by createdAt desc").page(0, limit).list()

    fun findByFilters(query: String?, tag: String?, limit: Int = 50): List<RunbookFragmentEntity> {
        val params = mutableMapOf<String, Any>()
        val conditions = mutableListOf<String>()

        if (!query.isNullOrBlank()) {
            conditions.add("(lower(title) like :query or lower(content) like :query)")
            params["query"] = "%${query.lowercase()}%"
        }

        if (!tag.isNullOrBlank() && tag != "Category") {
            conditions.add("lower(tags) like :tag")
            params["tag"] = "%${tag.lowercase()}%"
        }

        val queryStr = if (conditions.isEmpty()) {
            "order by createdAt desc"
        } else {
            conditions.joinToString(" and ") + " order by createdAt desc"
        }

        return find(queryStr, params).page(0, limit).list()
    }
}
