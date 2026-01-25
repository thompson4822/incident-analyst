package com.example.incidentanalyst.diagnosis

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import com.example.incidentanalyst.incident.IncidentEntity
import com.example.incidentanalyst.incident.IncidentId
import java.time.Instant

@QuarkusTest
class DiagnosisResourceTest {

    @InjectMock
    lateinit var diagnosisServiceMock: DiagnosisService

    private val diagnosisService: DiagnosisService
        get() = diagnosisServiceMock

    @BeforeEach
    fun setup() {
        reset(diagnosisServiceMock)
    }

    @Test
    fun `GET returns 200 with list of diagnoses`() {
        // Arrange
        val baseTime = Instant.now()
        val diagnosis1 = Diagnosis(
            id = DiagnosisId(1L),
            incidentId = IncidentId(10L),
            rootCause = "Root cause 1",
            steps = listOf("Step 1", "Step 2"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        val diagnosis2 = Diagnosis(
            id = DiagnosisId(2L),
            incidentId = IncidentId(20L),
            rootCause = "Root cause 2",
            steps = listOf("Step A"),
            confidence = Confidence.MEDIUM,
            verification = DiagnosisVerification.Unverified,
            createdAt = baseTime.minusSeconds(3600)
        )
        `when`(diagnosisServiceMock.listRecent()).thenReturn(listOf(diagnosis1, diagnosis2))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].id", equalTo(1))
            .body("[0].rootCause", equalTo("Root cause 1"))
            .body("[1].id", equalTo(2))
            .body("[1].rootCause", equalTo("Root cause 2"))
    }

    @Test
    fun `GET returns empty list when no diagnoses exist`() {
        // Arrange
        `when`(diagnosisServiceMock.listRecent()).thenReturn(emptyList())

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0))
    }

    @Test
    fun `GET by id returns 200 with diagnosis DTO for valid ID`() {
        // Arrange
        val testId = 123L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "CPU spike caused by runaway process",
            steps = listOf("Check CPU usage", "Identify process", "Kill process"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("incidentId", equalTo(10))
            .body("rootCause", equalTo("CPU spike caused by runaway process"))
            .body("steps.size()", equalTo(3))
            .body("steps[0]", equalTo("Check CPU usage"))
            .body("steps[1]", equalTo("Identify process"))
            .body("steps[2]", equalTo("Kill process"))
            .body("confidence", equalTo("HIGH"))
            .body("verified", equalTo(true))
    }

    @Test
    fun `GET by id returns 404 for non-existent diagnosis`() {
        // Arrange
        val testId = 999L
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Failure(DiagnosisError.NotFound))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(404)
    }

    @Test
    fun `GET by id returns 400 for invalid negative ID`() {
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/-1")
            .then()
            .statusCode(400)
            .body("error", notNullValue())
    }

    @Test
    fun `GET by id returns 400 for invalid zero ID`() {
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/0")
            .then()
            .statusCode(400)
            .body("error", notNullValue())
    }

    @Test
    fun `GET by id returns 400 with appropriate error message for negative ID`() {
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/-5")
            .then()
            .statusCode(400)
            .body("error", equalTo("Diagnosis ID must be a positive number"))
    }

    @Test
    fun `GET by id returns verified false for Unverified diagnosis`() {
        // Arrange
        val testId = 456L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.MEDIUM,
            verification = DiagnosisVerification.Unverified,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(200)
            .body("verified", equalTo(false))
    }

    @Test
    fun `GET by id maps all confidence levels correctly`() {
        // Arrange
        val confidences = listOf(Confidence.HIGH, Confidence.MEDIUM, Confidence.LOW, Confidence.UNKNOWN)
        confidences.forEach { confidence ->
            val testId = System.currentTimeMillis() % 10000
            val baseTime = Instant.now()
            val diagnosis = Diagnosis(
                id = DiagnosisId(testId),
                incidentId = IncidentId(10L),
                rootCause = "Root cause",
                steps = listOf("Step 1"),
                confidence = confidence,
                verification = DiagnosisVerification.Unverified,
                createdAt = baseTime
            )
            `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
                .thenReturn(DiagnosisResult.Success(diagnosis))

            // Act & Assert
            given()
                .contentType(ContentType.JSON)
                .`when`()
                .get("/diagnoses/$testId")
                .then()
                .statusCode(200)
                .body("confidence", equalTo(confidence.name))
        }
    }

    @Test
    fun `PUT verification endpoint returns 200 with updated DTO`() {
        // Arrange
        val testId = 123L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.updateVerification(DiagnosisId(testId), DiagnosisVerification.VerifiedByHuman))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"verified": true}""")
            .`when`()
            .put("/diagnoses/$testId/verification")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("verified", equalTo(true))
    }

    @Test
    fun `PUT verification endpoint returns 404 for non-existent diagnosis`() {
        // Arrange
        val testId = 999L
        `when`(diagnosisServiceMock.updateVerification(DiagnosisId(testId), DiagnosisVerification.VerifiedByHuman))
            .thenReturn(DiagnosisResult.Failure(DiagnosisError.NotFound))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"verified": true}""")
            .`when`()
            .put("/diagnoses/$testId/verification")
            .then()
            .statusCode(404)
    }

    @Test
    fun `PUT verification endpoint returns 400 for invalid ID`() {
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"verified": true}""")
            .`when`()
            .put("/diagnoses/-1/verification")
            .then()
            .statusCode(400)
            .body("error", equalTo("Diagnosis ID must be a positive number"))
    }

    @Test
    fun `PUT verification endpoint sets verified to true`() {
        // Arrange
        val testId = 789L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.MEDIUM,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.updateVerification(DiagnosisId(testId), DiagnosisVerification.VerifiedByHuman))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"verified": true}""")
            .`when`()
            .put("/diagnoses/$testId/verification")
            .then()
            .statusCode(200)
            .body("verified", equalTo(true))
    }

    @Test
    fun `PUT verification endpoint sets verified to false`() {
        // Arrange
        val testId = 321L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.LOW,
            verification = DiagnosisVerification.Unverified,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.updateVerification(DiagnosisId(testId), DiagnosisVerification.Unverified))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"verified": false}""")
            .`when`()
            .put("/diagnoses/$testId/verification")
            .then()
            .statusCode(200)
            .body("verified", equalTo(false))
    }

    @Test
    fun `PUT verification endpoint returns 500 on UpdateFailed`() {
        // Arrange
        val testId = 555L
        `when`(diagnosisServiceMock.updateVerification(DiagnosisId(testId), DiagnosisVerification.VerifiedByHuman))
            .thenReturn(DiagnosisResult.Failure(DiagnosisError.UpdateFailed))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"verified": true}""")
            .`when`()
            .put("/diagnoses/$testId/verification")
            .then()
            .statusCode(500)
    }

    @Test
    fun `PUT verification endpoint returns 400 for zero ID`() {
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"verified": true}""")
            .`when`()
            .put("/diagnoses/0/verification")
            .then()
            .statusCode(400)
            .body("error", equalTo("Diagnosis ID must be a positive number"))
    }

    @Test
    fun `GET by id returns correct diagnosisId in response`() {
        // Arrange
        val testId = 111L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
    }

    @Test
    fun `GET by id returns correct rootCause in response`() {
        // Arrange
        val testId = 222L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Custom root cause description",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(200)
            .body("rootCause", equalTo("Custom root cause description"))
    }

    @Test
    fun `GET by id returns correct steps in response`() {
        // Arrange
        val testId = 333L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step A", "Step B", "Step C"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(200)
            .body("steps.size()", equalTo(3))
            .body("steps[0]", equalTo("Step A"))
            .body("steps[1]", equalTo("Step B"))
            .body("steps[2]", equalTo("Step C"))
    }

    @Test
    fun `GET by id returns correct confidence in response`() {
        // Arrange
        val testId = 444L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.LOW,
            verification = DiagnosisVerification.VerifiedByHuman,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(200)
            .body("confidence", equalTo("LOW"))
    }

    @Test
    fun `GET by id returns correct verified flag in response`() {
        // Arrange
        val testId = 555L
        val baseTime = Instant.now()
        val diagnosis = Diagnosis(
            id = DiagnosisId(testId),
            incidentId = IncidentId(10L),
            rootCause = "Root cause",
            steps = listOf("Step 1"),
            confidence = Confidence.HIGH,
            verification = DiagnosisVerification.Unverified,
            createdAt = baseTime
        )
        `when`(diagnosisServiceMock.getById(DiagnosisId(testId)))
            .thenReturn(DiagnosisResult.Success(diagnosis))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/diagnoses/$testId")
            .then()
            .statusCode(200)
            .body("verified", equalTo(false))
    }
}
