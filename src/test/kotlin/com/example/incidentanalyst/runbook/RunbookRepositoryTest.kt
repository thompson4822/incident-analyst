package com.example.incidentanalyst.runbook

import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore

@QuarkusTest
class RunbookRepositoryTest {

    @Inject
    lateinit var runbookFragmentRepository: RunbookFragmentRepository

    @Inject
    lateinit var embeddingStore: EmbeddingStore<TextSegment>

    @BeforeEach
    @Transactional
    fun cleanup() {
        runbookFragmentRepository.deleteAll()
    }

    @Test
    @TestTransaction
    fun `findRecent returns fragments ordered by createdAt desc`() {
        // Arrange
        val baseTime = Instant.now()

        val oldFragment = RunbookFragmentEntity(
            title = "Old Fragment",
            content = "Old content",
            tags = null,
            createdAt = baseTime.minusSeconds(3600)
        )
        runbookFragmentRepository.persist(oldFragment)

        val newFragment = RunbookFragmentEntity(
            title = "New Fragment",
            content = "New content",
            tags = null,
            createdAt = baseTime
        )
        runbookFragmentRepository.persist(newFragment)
        runbookFragmentRepository.flush()

        // Act
        val result = runbookFragmentRepository.findRecent(50)

        // Assert
        assertEquals(2, result.size)
        assertEquals(newFragment.id, result[0].id)
        assertEquals(oldFragment.id, result[1].id)
    }

    @Test
    @TestTransaction
    fun `findRecent respects limit parameter`() {
        // Arrange
        val baseTime = Instant.now()

        // Use unique prefix to identify fragments created in this test
        val testPrefix = "LimitTest_${System.currentTimeMillis()}_"

        val testFragmentIds = (1..10).map { i ->
            RunbookFragmentEntity(
                title = "$testPrefix Fragment $i",
                content = "Content $i",
                tags = null,
                createdAt = baseTime.minusSeconds((10 - i).toLong())
            ).also { runbookFragmentRepository.persist(it) }
        }
        runbookFragmentRepository.flush()
        
        // Act - test with limit of 5
        val resultLimit5 = runbookFragmentRepository.findRecent(5)
        val resultLimit10 = runbookFragmentRepository.findRecent(10)

        // Assert
        // Verify that limit parameter is respected
        assertTrue(resultLimit5.size <= 5, "findRecent(5) should return at most 5 items, got ${resultLimit5.size}")
        assertTrue(resultLimit10.size <= 10, "findRecent(10) should return at most 10 items, got ${resultLimit10.size}")
        
        // Verify that more results are returned with higher limit
        assertTrue(resultLimit10.size >= resultLimit5.size, "findRecent(10) should return at least as many items as findRecent(5)")
        
        // Filter to only our test fragments to verify they're in the results
        val ourFragmentsLimit5 = resultLimit5.filter { it.title.startsWith(testPrefix) }
        val ourFragmentsLimit10 = resultLimit10.filter { it.title.startsWith(testPrefix) }
        
        // Verify our test fragments are present
        assertTrue(ourFragmentsLimit5.isNotEmpty(), "Should have at least one test fragment in limit 5 results")
        assertTrue(ourFragmentsLimit10.isNotEmpty(), "Should have at least one test fragment in limit 10 results")
        
        // Verify ordering - results should be ordered by createdAt desc
        val allResults = runbookFragmentRepository.findRecent(50)
        for (i in 0 until allResults.size - 1) {
            assertTrue(
                allResults[i].createdAt >= allResults[i + 1].createdAt,
                "Results should be ordered by createdAt descending"
            )
        }
    }

    @Test
    @TestTransaction
    fun `findRecent returns empty list when no fragments exist`() {
        // Act
        val result = runbookFragmentRepository.findRecent(50)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    @TestTransaction
    fun `findById returns entity for existing ID`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            title = "Test Fragment",
            content = "Test content",
            tags = "tag1,tag2",
            createdAt = Instant.now()
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()
        assertNotNull(entity.id)

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)

        // Assert
        assertNotNull(foundEntity)
        assertEquals(entity.id, foundEntity!!.id)
        assertEquals("Test Fragment", foundEntity.title)
        assertEquals("Test content", foundEntity.content)
        assertEquals("tag1,tag2", foundEntity.tags)
    }

    @Test
    @TestTransaction
    fun `findById returns null for non-existent ID`() {
        // Act
        val foundEntity = runbookFragmentRepository.findById(999999L)

        // Assert
        assertNull(foundEntity)
    }

    @Test
    @TestTransaction
    fun `toDomain maps all entity fields correctly`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(7200)
        val entity = RunbookFragmentEntity(
            title = "Test Title",
            content = "Test Content",
            tags = "tag1,tag2,tag3",
            createdAt = createdTime
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals(entity.id!!.toLong(), domain.id.value)
        assertEquals("Test Title", domain.title)
        assertEquals("Test Content", domain.content)
        assertEquals("tag1,tag2,tag3", domain.tags)
        assertEquals(createdTime, domain.createdAt)
    }

    @Test
    @TestTransaction
    fun `toDomain handles null tags correctly`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            title = "No Tags Fragment",
            content = "Content without tags",
            tags = null,
            createdAt = Instant.now()
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals(null, domain.tags)
        assertEquals("No Tags Fragment", domain.title)
        assertEquals("Content without tags", domain.content)
    }

    @Test
    @TestTransaction
    fun `toDomain preserves timestamp fields`() {
        // Arrange
        val createdTime = Instant.now().minusSeconds(3600)
        val entity = RunbookFragmentEntity(
            title = "Timestamp Test",
            content = "Content",
            tags = null,
            createdAt = createdTime
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals(createdTime, domain.createdAt)
    }

    @Test
    @TestTransaction
    fun `toDomain handles empty tags string`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            title = "Empty Tags Fragment",
            content = "Content",
            tags = "",
            createdAt = Instant.now()
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals("", domain.tags)
    }

    @Test
    @TestTransaction
    fun `toDomain handles single tag`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            title = "Single Tag Fragment",
            content = "Content",
            tags = "single-tag",
            createdAt = Instant.now()
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals("single-tag", domain.tags)
    }

    @Test
    @TestTransaction
    fun `toDomain handles long content`() {
        // Arrange
        val longContent = "A".repeat(5000)
        val entity = RunbookFragmentEntity(
            title = "Long Content Fragment",
            content = longContent,
            tags = null,
            createdAt = Instant.now()
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals(longContent, domain.content)
        assertEquals(5000, domain.content.length)
    }

    @Test
    @TestTransaction
    fun `toDomain handles special characters in tags`() {
        // Arrange
        val entity = RunbookFragmentEntity(
            title = "Special Tags Fragment",
            content = "Content",
            tags = "tag-1,tag_2,tag.3",
            createdAt = Instant.now()
        )
        runbookFragmentRepository.persist(entity)
        runbookFragmentRepository.flush()

        // Act
        val foundEntity = runbookFragmentRepository.findById(entity.id!!)
        val domain = foundEntity!!.toDomain()

        // Assert
        assertEquals("tag-1,tag_2,tag.3", domain.tags)
    }
}
