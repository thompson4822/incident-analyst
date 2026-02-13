package com.example.incidentanalyst.remediation

import com.example.incidentanalyst.diagnosis.Confidence
import com.example.incidentanalyst.diagnosis.Diagnosis
import com.example.incidentanalyst.diagnosis.DiagnosisId
import com.example.incidentanalyst.diagnosis.DiagnosisService
import com.example.incidentanalyst.diagnosis.DiagnosisVerification
import com.example.incidentanalyst.incident.IncidentId
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.Instant

@QuarkusTest
class RemediationResourceTest {

    @InjectMock
    lateinit var diagnosisService: DiagnosisService

    @InjectMock
    lateinit var remediationService: RemediationService

    @BeforeEach
    fun setup() {
        reset(diagnosisService, remediationService)
    }

    // ========== POST /start Tests ==========

    @Test
    fun `POST start returns 400 when no diagnosis exists`() {
        whenever(diagnosisService.getByIncidentId(999L)).thenReturn(null)

        given()
            .contentType(ContentType.JSON)
            .`when`()
            .post("/incidents/999/remediation/start")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST start returns 200 and HTML when diagnosis exists`() {
        val incidentId = 123L
        val diagnosis = createDiagnosis(DiagnosisId(1L), IncidentId(incidentId))
        val progress = RemediationProgress(
            incidentId = incidentId,
            diagnosisId = 1L,
            steps = listOf(
                RemediationStep("step-1", "Test step", status = StepStatus.COMPLETED)
            ),
            currentStepIndex = 0,
            status = ExecutionStatus.COMPLETED,
            startedAt = Instant.now(),
            completedAt = Instant.now()
        )

        whenever(diagnosisService.getByIncidentId(incidentId)).thenReturn(diagnosis)
        whenever(remediationService.getProgress(IncidentId(incidentId))).thenReturn(progress)

        given()
            .contentType(ContentType.JSON)
            .`when`()
            .post("/incidents/$incidentId/remediation/start")
            .then()
            .statusCode(200)
            .contentType(ContentType.HTML)
    }

    @Test
    fun `POST start returns 500 when progress is null after execution`() {
        val incidentId = 124L
        val diagnosis = createDiagnosis(DiagnosisId(2L), IncidentId(incidentId))

        whenever(diagnosisService.getByIncidentId(incidentId)).thenReturn(diagnosis)
        whenever(remediationService.getProgress(IncidentId(incidentId))).thenReturn(null)

        given()
            .contentType(ContentType.JSON)
            .`when`()
            .post("/incidents/$incidentId/remediation/start")
            .then()
            .statusCode(500)
    }

    // ========== GET /progress Tests ==========

    @Test
    fun `GET progress returns 200 with message when no remediation in progress`() {
        whenever(remediationService.getProgress(IncidentId(999L))).thenReturn(null)

        given()
            .`when`()
            .get("/incidents/999/remediation/progress")
            .then()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .body(containsString("No remediation in progress"))
    }

    @Test
    fun `GET progress returns 200 with HTML when remediation exists`() {
        val incidentId = 456L
        val progress = RemediationProgress(
            incidentId = incidentId,
            diagnosisId = 1L,
            steps = listOf(
                RemediationStep("step-1", "First step", status = StepStatus.COMPLETED)
            ),
            currentStepIndex = 0,
            status = ExecutionStatus.COMPLETED,
            startedAt = Instant.now(),
            completedAt = Instant.now()
        )

        whenever(remediationService.getProgress(IncidentId(incidentId))).thenReturn(progress)

        given()
            .`when`()
            .get("/incidents/$incidentId/remediation/progress")
            .then()
            .statusCode(200)
            .contentType(ContentType.HTML)
    }

    @Test
    fun `GET progress returns HTML with step count for completed remediation`() {
        val incidentId = 789L
        val progress = RemediationProgress(
            incidentId = incidentId,
            diagnosisId = 1L,
            steps = listOf(
                RemediationStep("step-1", "First", status = StepStatus.COMPLETED),
                RemediationStep("step-2", "Second", status = StepStatus.COMPLETED)
            ),
            currentStepIndex = 1,
            status = ExecutionStatus.COMPLETED,
            startedAt = Instant.now().minusSeconds(60),
            completedAt = Instant.now()
        )

        whenever(remediationService.getProgress(IncidentId(incidentId))).thenReturn(progress)

        given()
            .`when`()
            .get("/incidents/$incidentId/remediation/progress")
            .then()
            .statusCode(200)
            .body(containsString("2 steps"))
    }

    @Test
    fun `GET progress returns HTML with error message when failed`() {
        val incidentId = 888L
        val progress = RemediationProgress(
            incidentId = incidentId,
            diagnosisId = 1L,
            steps = listOf(
                RemediationStep("step-1", "Failed step", status = StepStatus.FAILED)
            ),
            currentStepIndex = 0,
            status = ExecutionStatus.FAILED,
            startedAt = Instant.now().minusSeconds(60),
            completedAt = Instant.now(),
            errorMessage = "Something went wrong"
        )

        whenever(remediationService.getProgress(IncidentId(incidentId))).thenReturn(progress)

        given()
            .`when`()
            .get("/incidents/$incidentId/remediation/progress")
            .then()
            .statusCode(200)
            .body(containsString("Something went wrong"))
    }

    // ========== Helper Functions ==========

    private fun createDiagnosis(id: DiagnosisId, incidentId: IncidentId): Diagnosis {
        return Diagnosis(
            id = id,
            incidentId = incidentId,
            rootCause = "Test root cause",
            steps = listOf("Step 1", "Step 2"),
            structuredSteps = emptyList(),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = Instant.now()
        )
    }
}
