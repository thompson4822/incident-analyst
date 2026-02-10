package com.example.incidentanalyst.config

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
class ProfileServiceTest {

    @Inject
    lateinit var profileService: ProfileService

    @Test
    fun `getProfile returns configured application profile`() {
        // Act
        val profile = profileService.getProfile()

        // Assert
        // These match the default values in ProfileService.kt or application.properties
        assertTrue(profile.name.isNotEmpty())
        assertTrue(profile.stack.isNotEmpty())
        assertTrue(profile.components.isNotEmpty())
        assertTrue(profile.primaryRegion.isNotEmpty())
    }

    @Test
    fun `profile stack is correctly parsed from comma-separated string`() {
        // Arrange
        profileService.stack = "Kotlin, Quarkus, Postgres"
        
        // Act
        val profile = profileService.getProfile()
        
        // Assert
        assertEquals(3, profile.stack.size)
        assertEquals("Kotlin", profile.stack[0])
        assertEquals("Quarkus", profile.stack[1])
        assertEquals("Postgres", profile.stack[2])
    }

    @Test
    fun `profile components are correctly parsed from comma-separated string`() {
        // Arrange
        profileService.components = "API Gateway, Database, Auth Service"
        
        // Act
        val profile = profileService.getProfile()
        
        // Assert
        assertEquals(3, profile.components.size)
        assertEquals("API Gateway", profile.components[0])
        assertEquals("Database", profile.components[1])
        assertEquals("Auth Service", profile.components[2])
    }
}
