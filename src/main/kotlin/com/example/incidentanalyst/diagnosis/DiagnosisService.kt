package com.example.incidentanalyst.diagnosis

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class DiagnosisService(
    private val diagnosisRepository: DiagnosisRepository
)
