package com.example.incidentanalyst.config

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

data class ApplicationProfile(
    val name: String,
    val stack: List<String>,
    val components: List<String>,
    val primaryRegion: String
)

@ApplicationScoped
class ProfileService {

    @ConfigProperty(name = "app.profile.name", defaultValue = "Generic SaaS App")
    lateinit var name: String

    @ConfigProperty(name = "app.profile.stack", defaultValue = "Kotlin, Quarkus, Postgres")
    lateinit var stack: String

    @ConfigProperty(name = "app.profile.components", defaultValue = "API Gateway, Database, Auth Service")
    lateinit var components: String

    @ConfigProperty(name = "app.profile.region", defaultValue = "us-east-1")
    lateinit var region: String

    fun getProfile(): ApplicationProfile {
        return ApplicationProfile(
            name = name,
            stack = stack.split(",").map { it.trim() },
            components = components.split(",").map { it.trim() },
            primaryRegion = region
        )
    }
}
