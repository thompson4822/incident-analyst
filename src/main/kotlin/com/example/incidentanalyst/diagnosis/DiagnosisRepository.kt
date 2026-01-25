package com.example.incidentanalyst.diagnosis

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class DiagnosisRepository : PanacheRepository<DiagnosisEntity>
