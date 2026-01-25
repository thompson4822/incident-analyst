package com.example.incidentanalyst.runbook

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class RunbookDtoTest {

    @Test
    fun `toResponseDto maps id correctly`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(123L),
            title = "Title",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals(123L, dto.id)
    }

    @Test
    fun `toResponseDto maps title correctly`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Test Title",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals("Test Title", dto.title)
    }

    @Test
    fun `toResponseDto maps content correctly`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Title",
            content = "Test Content",
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals("Test Content", dto.content)
    }

    @Test
    fun `toResponseDto maps tags correctly with non-null value`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Title",
            content = "Content",
            tags = "tag1,tag2,tag3",
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals("tag1,tag2,tag3", dto.tags)
    }

    @Test
    fun `toResponseDto maps tags correctly with null value`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Title",
            content = "Content",
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals(null, dto.tags)
    }

    @Test
    fun `toResponseDto maps all fields correctly`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(999L),
            title = "Complete Title",
            content = "Complete Content",
            tags = "complete,tags",
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals(999L, dto.id)
        assertEquals("Complete Title", dto.title)
        assertEquals("Complete Content", dto.content)
        assertEquals("complete,tags", dto.tags)
    }

    @Test
    fun `RunbookFragmentUpdateRequestDto accepts title`() {
        // Arrange & Act
        val request = RunbookFragmentUpdateRequestDto(
            title = "Update Title",
            content = "Content",
            tags = null
        )

        // Assert
        assertEquals("Update Title", request.title)
    }

    @Test
    fun `RunbookFragmentUpdateRequestDto accepts content`() {
        // Arrange & Act
        val request = RunbookFragmentUpdateRequestDto(
            title = "Title",
            content = "Update Content",
            tags = null
        )

        // Assert
        assertEquals("Update Content", request.content)
    }

    @Test
    fun `RunbookFragmentUpdateRequestDto accepts tags with non-null value`() {
        // Arrange & Act
        val request = RunbookFragmentUpdateRequestDto(
            title = "Title",
            content = "Content",
            tags = "tag1,tag2"
        )

        // Assert
        assertEquals("tag1,tag2", request.tags)
    }

    @Test
    fun `RunbookFragmentUpdateRequestDto accepts tags with null value`() {
        // Arrange & Act
        val request = RunbookFragmentUpdateRequestDto(
            title = "Title",
            content = "Content",
            tags = null
        )

        // Assert
        assertEquals(null, request.tags)
    }

    @Test
    fun `RunbookFragmentUpdateRequestDto handles empty tags`() {
        // Arrange & Act
        val request = RunbookFragmentUpdateRequestDto(
            title = "Title",
            content = "Content",
            tags = ""
        )

        // Assert
        assertEquals("", request.tags)
    }

    @Test
    fun `RunbookFragmentResponseDto includes all fields`() {
        // Arrange & Act
        val dto = RunbookFragmentResponseDto(
            id = 123L,
            title = "Title",
            content = "Content",
            tags = "tag1,tag2"
        )

        // Assert
        assertEquals(123L, dto.id)
        assertEquals("Title", dto.title)
        assertEquals("Content", dto.content)
        assertEquals("tag1,tag2", dto.tags)
    }

    @Test
    fun `RunbookFragmentResponseDto handles null tags`() {
        // Arrange & Act
        val dto = RunbookFragmentResponseDto(
            id = 123L,
            title = "Title",
            content = "Content",
            tags = null
        )

        // Assert
        assertEquals(null, dto.tags)
    }

    @Test
    fun `RunbookFragmentUpdateRequestDto includes all fields`() {
        // Arrange & Act
        val request = RunbookFragmentUpdateRequestDto(
            title = "Update Title",
            content = "Update Content",
            tags = "update,tags"
        )

        // Assert
        assertEquals("Update Title", request.title)
        assertEquals("Update Content", request.content)
        assertEquals("update,tags", request.tags)
    }

    @Test
    fun `Tags are preserved in DTO as String`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Title",
            content = "Content",
            tags = "tag1,tag2,tag3,tag4",
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals("tag1,tag2,tag3,tag4", dto.tags)
        assertNotNull(dto.tags)
    }

    @Test
    fun `toResponseDto handles single tag`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Title",
            content = "Content",
            tags = "single-tag",
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals("single-tag", dto.tags)
    }

    @Test
    fun `toResponseDto handles special characters in tags`() {
        // Arrange
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Title",
            content = "Content",
            tags = "tag-1,tag_2,tag.3",
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals("tag-1,tag_2,tag.3", dto.tags)
    }

    @Test
    fun `toResponseDto handles long content`() {
        // Arrange
        val longContent = "A".repeat(5000)
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Long Content",
            content = longContent,
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals(longContent, dto.content)
        assertEquals(5000, dto.content.length)
    }

    @Test
    fun `toResponseDto handles Unicode characters in content`() {
        // Arrange
        val unicodeContent = "Hello 世界 🌍"
        val fragment = RunbookFragment(
            id = RunbookFragmentId(1L),
            title = "Unicode Content",
            content = unicodeContent,
            tags = null,
            createdAt = Instant.now()
        )

        // Act
        val dto = fragment.toResponseDto()

        // Assert
        assertEquals(unicodeContent, dto.content)
    }

    @Test
    fun `RunbookFragmentResponseDto handles zero id`() {
        // Arrange & Act
        val dto = RunbookFragmentResponseDto(
            id = 0L,
            title = "Title",
            content = "Content",
            tags = null
        )

        // Assert
        assertEquals(0L, dto.id)
    }

    @Test
    fun `RunbookFragmentResponseDto handles large id`() {
        // Arrange & Act
        val dto = RunbookFragmentResponseDto(
            id = 999999999L,
            title = "Title",
            content = "Content",
            tags = null
        )

        // Assert
        assertEquals(999999999L, dto.id)
    }
}
