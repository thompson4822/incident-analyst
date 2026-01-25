package com.example.incidentanalyst.runbook

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RunbookService(
    private val runbookFragmentRepository: RunbookFragmentRepository
)
