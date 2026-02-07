package com.example.incidentanalyst.aws

import com.example.incidentanalyst.incident.Severity
import java.time.Instant

data class CloudWatchTestDataRequestDto(
    val count: Int? = 10,
    val namespaces: List<String>? = null,
    val severities: List<Severity>? = null,
    val minSeverity: Severity? = null,
    val maxSeverity: Severity? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val seed: Long? = null
)

data class CloudWatchTestDataResponseDto(
    val generatedCount: Int,
    val severityBreakdown: Map<String, Int>,
    val createdIncidentIds: List<Long>,
    val seedUsed: Long?
)
