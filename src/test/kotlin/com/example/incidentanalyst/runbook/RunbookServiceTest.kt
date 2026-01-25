package com.example.incidentanalyst.runbook

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import java.time.Instant

@QuarkusTest
class RunbookServiceTest {

    @InjectMock
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @Inject
    lateinit var runbookService: RunbookService

    @BeforeEach
    fun setup() {
        reset(runbookFragmentRepository)
    }

    @Test
    fun `listRecent returns fragments ordered by createdAt desc`() {
        // Arrange
        val baseTime = Instant.now()

        val oldFragment = RunbookFragmentEntity(
            id = 1L,
            title = "Old Fragment",
            content = "Old content",
            tags = null,
            createdAt = baseTime.minusSeconds(3600)
        )

        val newFragment = RunbookFragmentEntity(
            id = 2L,
            title = "New Fragment",
            content = "New content",
            tags = null,
            createdAt = baseTime
        )

        `when`(runbookFragmentRepository.findRecent(50)).thenReturn(listOf(newFragment, oldFragment))

        // Act
        val result = runbookService.listRecent()

        // Assert
        assertEquals(2, result.size)
        assertEquals(2L, result[0].id.value)
        assertEquals(1L, result[1].id.value)
    }

    @Test
    fun `listRecent with limit parameter works`() {
        // Arrange
        val baseTime = Instant.now()

        val fragments = (1..10).map { i ->
            RunbookFragmentEntity(
                id = i.toLong(),
                title = "Fragment $i",
                content = "Content $i",
                tags = null,
                createdAt = baseTime.minusSeconds((10 - i).toLong())
            )
        }

        `when`(runbookFragmentRepository.findRecent(5)).thenReturn(fragments.take(5))

        // Act
        val result = runbookService.listRecent(5)

        // Assert
        assertEquals(5, result.size)
        assertEquals(1L, result[0].id.value)
        assertEquals(5L, result[4].id.value)
    }

    @Test
    fun `listRecent returns empty list when no fragments exist`() {
        // Arrange
        `when`(runbookFragmentRepository.findRecent(50)).thenReturn(emptyList())

        // Act
        val result = runbookService.listRecent()

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `getById returns Success for valid fragment`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = RunbookFragmentEntity(
            id = testId,
            title = "Test Fragment",
            content = "Test content with details",
            tags = "tag1,tag2",
            createdAt = testTimestamp
        )
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = runbookService.getById(RunbookFragmentId(testId))

        // Assert
        assertTrue(result is RunbookFragmentResult.Success)
        val fragment = (result as RunbookFragmentResult.Success).fragment
        assertEquals(testId, fragment.id.value)
        assertEquals("Test Fragment", fragment.title)
        assertEquals("Test content with details", fragment.content)
        assertEquals("tag1,tag2", fragment.tags)
    }

    @Test
    fun `getById returns Failure NotFound for non-existent fragment`() {
        // Arrange
        val testId = 999L
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(null)

        // Act
        val result = runbookService.getById(RunbookFragmentId(testId))

        // Assert
        assertTrue(result is RunbookFragmentResult.Failure)
        val error = (result as RunbookFragmentResult.Failure).error
        assertTrue(error is RunbookFragmentError.NotFound)
    }

    @Test
    fun `updateFragment updates title, content, tags successfully`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = RunbookFragmentEntity(
            id = testId,
            title = "Original Title",
            content = "Original Content",
            tags = "old,tag",
            createdAt = testTimestamp
        )
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Updated Title",
            "Updated Content",
            "new,tag"
        )

        // Assert
        assertTrue(result is RunbookFragmentResult.Success)
        val fragment = (result as RunbookFragmentResult.Success).fragment
        assertEquals("Updated Title", fragment.title)
        assertEquals("Updated Content", fragment.content)
        assertEquals("new,tag", fragment.tags)
        assertEquals("Updated Title", entity.title)
        assertEquals("Updated Content", entity.content)
        assertEquals("new,tag", entity.tags)
    }

    @Test
    fun `updateFragment returns Failure NotFound for non-existent fragment`() {
        // Arrange
        val testId = 999L
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(null)

        // Act
        val result = runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Title",
            "Content",
            null
        )

        // Assert
        assertTrue(result is RunbookFragmentResult.Failure)
        val error = (result as RunbookFragmentResult.Failure).error
        assertTrue(error is RunbookFragmentError.NotFound)
    }

    @Test
    fun `updateFragment returns Failure ValidationFailed for blank title`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = RunbookFragmentEntity(
            id = testId,
            title = "Original Title",
            content = "Original Content",
            tags = null,
            createdAt = testTimestamp
        )
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = runbookService.updateFragment(
            RunbookFragmentId(testId),
            "   ",
            "Content",
            null
        )

        // Assert
        assertTrue(result is RunbookFragmentResult.Failure)
        val error = (result as RunbookFragmentResult.Failure).error
        assertTrue(error is RunbookFragmentError.ValidationFailed)
    }

    @Test
    fun `updateFragment returns Failure ValidationFailed for blank content`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = RunbookFragmentEntity(
            id = testId,
            title = "Original Title",
            content = "Original Content",
            tags = null,
            createdAt = testTimestamp
        )
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Title",
            "",
            null
        )

        // Assert
        assertTrue(result is RunbookFragmentResult.Failure)
        val error = (result as RunbookFragmentResult.Failure).error
        assertTrue(error is RunbookFragmentError.ValidationFailed)
    }

    @Test
    fun `updateFragment returns Failure ValidationFailed for both blank title and content`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = RunbookFragmentEntity(
            id = testId,
            title = "Original Title",
            content = "Original Content",
            tags = null,
            createdAt = testTimestamp
        )
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = runbookService.updateFragment(
            RunbookFragmentId(testId),
            "",
            "   ",
            null
        )

        // Assert
        assertTrue(result is RunbookFragmentResult.Failure)
        val error = (result as RunbookFragmentResult.Failure).error
        assertTrue(error is RunbookFragmentError.ValidationFailed)
    }

    @Test
    fun `updateFragment can set tags to null`() {
        // Arrange
        val testId = 123L
        val testTimestamp = Instant.now()
        val entity = RunbookFragmentEntity(
            id = testId,
            title = "Original Title",
            content = "Original Content",
            tags = "old,tag",
            createdAt = testTimestamp
        )
        `when`(runbookFragmentRepository.findById(testId)).thenReturn(entity)

        // Act
        val result = runbookService.updateFragment(
            RunbookFragmentId(testId),
            "Title",
            "Content",
            null
        )

        // Assert
        assertTrue(result is RunbookFragmentResult.Success)
        val fragment = (result as RunbookFragmentResult.Success).fragment
        assertEquals(null, fragment.tags)
        assertEquals(null, entity.tags)
    }
}
