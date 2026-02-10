package com.example.incidentanalyst.runbook

import com.example.incidentanalyst.common.Either
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

@QuarkusTest
class RunbookResourceTest {

    @InjectMock
    lateinit var runbookService: RunbookService

    @BeforeEach
    fun setup() {
        reset(runbookService)
    }

    @Test
    fun `GET returns 200 with list of fragments`() {
        // Arrange
        val baseTime = Instant.now()
        val fragment1 = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Fragment 1",
            content = "Content 1",
            tags = "tag1",
            createdAt = baseTime
        )
        val fragment2 = RunbookFragment(
            id = RunbookFragmentId(2L),
            title = "Fragment 2",
            content = "Content 2",
            tags = "tag2",
            createdAt = baseTime.minusSeconds(3600)
        )
        whenever(runbookService.search(null, null)).thenReturn(listOf(fragment1, fragment2))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/runbooks")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].id", equalTo(1))
            .body("[0].title", equalTo("Fragment 1"))
            .body("[1].id", equalTo(2))
            .body("[1].title", equalTo("Fragment 2"))
    }

    @Test
    fun `GET returns empty list when no fragments exist`() {
        // Arrange
        whenever(runbookService.search(null, null)).thenReturn(emptyList())

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/runbooks")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0))
    }

    @Test
    fun `GET by id returns 200 with fragment DTO for valid ID`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "Test Fragment",
            content = "Test content with details",
            tags = "tag1,tag2",
            createdAt = testTimestamp
        )
        whenever(runbookService.getById(RunbookFragmentId(testId)))
            .thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("title", equalTo("Test Fragment"))
            .body("content", equalTo("Test content with details"))
            .body("tags", equalTo("tag1,tag2"))
    }

    @Test
    fun `GET by id returns 404 for non-existent fragment`() {
        // Arrange
        val testId = 999L
        whenever(runbookService.getById(RunbookFragmentId(testId)))
            .thenReturn(Either.Left(RunbookFragmentError.NotFound))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/runbooks/$testId")
            .then()
            .statusCode(404)
    }

    @Test
    fun `GET by id handles null tags correctly in response`() {
        // Arrange
        val testId = 456L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "No Tags Fragment",
            content = "Content without tags",
            tags = null,
            createdAt = testTimestamp
        )
        whenever(runbookService.getById(RunbookFragmentId(testId)))
            .thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("tags", nullValue())
    }

    @Test
    fun `PUT returns 200 with updated DTO`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "Updated Title",
            content = "Updated Content",
            tags = "updated,tag",
            createdAt = testTimestamp
        )
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Updated Title",
            "Updated Content",
            "updated,tag"
        )).thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "Updated Title", "content": "Updated Content", "tags": "updated,tag"}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("title", equalTo("Updated Title"))
            .body("content", equalTo("Updated Content"))
            .body("tags", equalTo("updated,tag"))
    }

    @Test
    fun `PUT returns 404 for non-existent fragment`() {
        // Arrange
        val testId = 999L
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Title",
            "Content",
            null
        )).thenReturn(Either.Left(RunbookFragmentError.NotFound))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "Title", "content": "Content", "tags": null}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(404)
    }

    @Test
    fun `PUT returns 400 for ValidationFailed error with blank title`() {
        // Arrange
        val testId = 123L
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "   ",
            "Content",
            null
        )).thenReturn(Either.Left(RunbookFragmentError.ValidationFailed))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "   ", "content": "Content", "tags": null}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(400)
            .body("error", equalTo("Invalid request data"))
    }

    @Test
    fun `PUT returns 400 for ValidationFailed error with blank content`() {
        // Arrange
        val testId = 123L
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Title",
            "",
            null
        )).thenReturn(Either.Left(RunbookFragmentError.ValidationFailed))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "Title", "content": "", "tags": null}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(400)
            .body("error", equalTo("Invalid request data"))
    }

    @Test
    fun `PUT returns 400 for ValidationFailed error with both blank`() {
        // Arrange
        val testId = 123L
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "",
            "   ",
            null
        )).thenReturn(Either.Left(RunbookFragmentError.ValidationFailed))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "", "content": "   ", "tags": null}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(400)
            .body("error", equalTo("Invalid request data"))
    }

    @Test
    fun `PUT updates title correctly`() {
        // Arrange
        val testId = 789L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "New Title",
            content = "Original Content",
            tags = null,
            createdAt = testTimestamp
        )
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "New Title",
            "Original Content",
            null
        )).thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "New Title", "content": "Original Content", "tags": null}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("title", equalTo("New Title"))
    }

    @Test
    fun `PUT updates content correctly`() {
        // Arrange
        val testId = 321L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "Original Title",
            content = "New Content",
            tags = null,
            createdAt = testTimestamp
        )
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Original Title",
            "New Content",
            null
        )).thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "Original Title", "content": "New Content", "tags": null}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("content", equalTo("New Content"))
    }

    @Test
    fun `PUT updates tags correctly`() {
        // Arrange
        val testId = 555L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "Title",
            content = "Content",
            tags = "new,tags",
            createdAt = testTimestamp
        )
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Title",
            "Content",
            "new,tags"
        )).thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "Title", "content": "Content", "tags": "new,tags"}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("tags", equalTo("new,tags"))
    }

    @Test
    fun `PUT can set tags to null`() {
        // Arrange
        val testId = 666L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "Title",
            content = "Content",
            tags = null,
            createdAt = testTimestamp
        )
        whenever(runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Title",
            "Content",
            null
        )).thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body("""{"title": "Title", "content": "Content", "tags": null}""")
            .`when`()
            .put("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("tags", nullValue())
    }

    @Test
    fun `Response includes correct fields for list endpoint`() {
        // Arrange
        val baseTime = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Test Fragment",
            content = "Test content",
            tags = "tag1,tag2",
            createdAt = baseTime
        )
        whenever(runbookService.search(null, null)).thenReturn(listOf(fragment))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/runbooks")
            .then()
            .statusCode(200)
            .body("[0].id", equalTo(1))
            .body("[0].title", equalTo("Test Fragment"))
            .body("[0].content", equalTo("Test content"))
            .body("[0].tags", equalTo("tag1,tag2"))
    }

    @Test
    fun `Response includes correct fields for get by id endpoint`() {
        // Arrange
        val testId = 777L
        val testTimestamp = Instant.now()
        val fragment = RunbookFragment(
            id = RunbookFragmentId(testId),
            title = "Get Test Fragment",
            content = "Get test content",
            tags = null,
            createdAt = testTimestamp
        )
        whenever(runbookService.getById(RunbookFragmentId(testId)))
            .thenReturn(Either.Right(fragment))

        // Act & Assert
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/runbooks/$testId")
            .then()
            .statusCode(200)
            .body("id", equalTo(testId.toInt()))
            .body("title", equalTo("Get Test Fragment"))
            .body("content", equalTo("Get test content"))
            .body("tags", nullValue())
    }
}
