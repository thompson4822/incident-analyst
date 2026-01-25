package com.example.incidentanalyst.runbook

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RunbookFragmentRepository : PanacheRepository<RunbookFragmentEntity>
