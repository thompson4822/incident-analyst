package com.example.incidentanalyst.runbook

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RunbookFragmentRepository : PanacheRepository<RunbookFragmentEntity> {

    fun findRecent(limit: Int = 50): List<RunbookFragmentEntity> =
        find("order by createdAt desc").page(0, limit).list()
}
