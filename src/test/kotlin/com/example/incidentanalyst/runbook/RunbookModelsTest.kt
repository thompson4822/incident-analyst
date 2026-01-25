package com.example.incidentanalyst.runbook

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RunbookModelsTest {

    @Test
    fun `RunbookFragmentId value class wraps Long value`() {
        // Arrange & Act
        val id = RunbookFragmentId(123L)

        // Assert
        assertEquals(123L, id.value)
    }

    @Test
    fun `RunbookFragmentId handles zero value`() {
        // Arrange & Act
        val id = RunbookFragmentId(0L)

        // Assert
        assertEquals(0L, id.value)
    }

    @Test
    fun `RunbookFragmentId handles large value`() {
        // Arrange & Act
        val id = RunbookFragmentId(999999999L)

        // Assert
        assertEquals(999999999L, id.value)
    }

    @Test
    fun `RunbookFragmentId handles negative value`() {
        // Arrange & Act
        val id = RunbookFragmentId(-1L)

        // Assert
        assertEquals(-1L, id.value)
    }

    @Test
    fun `RunbookFragment data class properties are correct`() {
        // Arrange
        val id = RunbookFragmentId(1L)
        val title = "Test Fragment"
        val content = "Test content"
        val tags = "tag1,tag2"
        val createdAt = Instant.now()

        // Act
        val fragment = RunbookFragment(
            id = id,
            title = title,
            content = content,
            tags = tags,
            createdAt = createdAt
        )

        // Assert
        assertEquals(id, fragment.id)
        assertEquals(title, fragment.title)
        assertEquals(content, fragment.content)
        assertEquals(tags, fragment.tags)
        assertEquals(createdAt, fragment.createdAt)
    }

    @Test
    fun `RunbookFragment handles null tags`() {
        // Arrange & Act
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Fragment",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )

        // Assert
        assertEquals(null, fragment.tags)
    }

    @Test
    fun `RunbookFragment handles empty tags`() {
        // Arrange & Act
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Fragment",
            content = "Content",
            tags = "",
            createdAt = Instant.now()
        )

        // Assert
        assertEquals("", fragment.tags)
    }

    @Test
    fun `RunbookFragmentError NotFound exists`() {
        // Arrange & Act
        val notFound: RunbookFragmentError = RunbookFragmentError.NotFound

        // Assert
        assertTrue(notFound is RunbookFragmentError.NotFound)
    }

    @Test
    fun `RunbookFragmentError ValidationFailed exists`() {
        // Arrange & Act
        val validationFailed: RunbookFragmentError = RunbookFragmentError.ValidationFailed

        // Assert
        assertTrue(validationFailed is RunbookFragmentError.ValidationFailed)
    }

    @Test
    fun `RunbookFragmentResult Success wraps fragment`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Fragment",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )
        val successResult: RunbookFragmentResult = RunbookFragmentResult.Success(fragment)

        // Assert
        assertTrue(successResult is RunbookFragmentResult.Success)
        assertEquals(fragment, (successResult as RunbookFragmentResult.Success).fragment)
    }

    @Test
    fun `RunbookFragmentResult Failure wraps error`() {
        // Arrange
        val failureResult: RunbookFragmentResult = RunbookFragmentResult.Failure(RunbookFragmentError.NotFound)

        // Assert
        assertTrue(failureResult is RunbookFragmentResult.Failure)
        assertTrue((failureResult as RunbookFragmentResult.Failure).error is RunbookFragmentError.NotFound)
    }

    @Test
    fun `RunbookFragmentResult Failure can wrap ValidationFailed`() {
        // Arrange
        val failureResult: RunbookFragmentResult = RunbookFragmentResult.Failure(RunbookFragmentError.ValidationFailed)

        // Assert
        assertTrue(failureResult is RunbookFragmentResult.Failure)
        assertTrue((failureResult as RunbookFragmentResult.Failure).error is RunbookFragmentError.ValidationFailed)
    }

    @Test
    fun `Pattern matching on RunbookFragmentResult Success works correctly`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Test",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )
        val successResult: RunbookFragmentResult = RunbookFragmentResult.Success(fragment)

        // Act & Assert
        var successHandled = false
        var failureHandled = false

        when (successResult) {
            is RunbookFragmentResult.Success -> successHandled = true
            is RunbookFragmentResult.Failure -> failureHandled = true
        }

        assertTrue(successHandled)
        assertTrue(!failureHandled)
    }

    @Test
    fun `Pattern matching on RunbookFragmentResult Failure works correctly`() {
        // Arrange
        val failureResult: RunbookFragmentResult = RunbookFragmentResult.Failure(RunbookFragmentError.NotFound)

        // Act & Assert
        var successHandled = false
        var failureHandled = false

        when (failureResult) {
            is RunbookFragmentResult.Success -> successHandled = true
            is RunbookFragmentResult.Failure -> failureHandled = true
        }

        assertTrue(!successHandled)
        assertTrue(failureHandled)
    }

    @Test
    fun `toDomain maps entity fields correctly`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(7200)
        val entity = RunbookFragmentEntity(
            id = 999L,
            title = "Custom Title",
            content = "Custom Content",
            tags = "custom,tags",
            createdAt = createdTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(999L, domain.id.value)
        assertEquals("Custom Title", domain.title)
        assertEquals("Custom Content", domain.content)
        assertEquals("custom,tags", domain.tags)
        assertEquals(createdTime, domain.createdAt)
    }

    @Test
    fun `toDomain handles null tags`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            id = 1L,
            title = "No Tags",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(null, domain.tags)
    }

    @Test
    fun `toDomain handles empty tags`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            id = 1L,
            title = "Empty Tags",
            content = "Content",
            tags = "",
            createdAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals("", domain.tags)
    }

    @Test
    fun `toDomain preserves timestamp`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(3600)
        val entity = RunbookFragmentEntity(
            id = 1L,
            title = "Timestamp Test",
            content = "Content",
            tags = null,
            createdAt = createdTime
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(createdTime, domain.createdAt)
    }

    @Test
    fun `toDomain handles long content`() {
        // Arrange
        val longContent = "A".repeat(5000)
        val entity = RunbookFragmentEntity(
            id = 1L,
            title = "Long Content",
            content = longContent,
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(longContent, domain.content)
        assertEquals(5000, domain.content.length)
    }

    @Test
    fun `toDomain handles special characters in tags`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            id = 1L,
            title = "Special Tags",
            content = "Content",
            tags = "tag-1,tag_2,tag.3",
            createdAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals("tag-1,tag_2,tag.3", domain.tags)
    }

    @Test
    fun `toDomain handles Unicode characters in content`() {
        // Arrange
        val unicodeContent = "Hello 世界 🌍"
        val entity = RunbookFragmentEntity(
            id = 1L,
            title = "Unicode Content",
            content = unicodeContent,
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(unicodeContent, domain.content)
    }

    @Test
    fun `RunbookFragment is immutable`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Title",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )

        // Assert - All properties are val (immutable)
        assertNotNull(fragment.id)
        assertNotNull(fragment.title)
        assertNotNull(fragment.content)
    }

    @Test
    fun `RunbookFragment with all fields populated`() {
        // Arrange
        val id = RunbookFragmentId(123L)
        val title = "Complete Fragment"
        val content = "Complete content with all fields"
        val tags = "tag1,tag2,tag3"
        val createdAt = Instant.now()

        // Act
        val fragment = RunbookFragment(
            id = id,
            title = title,
            content = content,
            tags = tags,
            createdAt = createdAt
        )

        // Assert
        assertEquals(id, fragment.id)
        assertEquals(title, fragment.title)
        assertEquals(content, fragment.content)
        assertEquals(tags, fragment.tags)
        assertEquals(createdAt, fragment.createdAt)
    }
}
