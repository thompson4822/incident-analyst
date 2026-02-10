# TICKET-013: Application Profile Configuration

## Overview
Move from hardcoded "AWS/CloudWatch" logic to a dynamic "Application Profile" that contextualizes the AI's understanding of the target system.

## Tasks
- [ ] Define an `ApplicationProfile` data class (name, stack, components, primary_region).
- [ ] Implement a `ProfileService` that loads this profile from `application.properties` or a YAML file.
- [ ] Update the `IncidentAnalystAgent` system prompt to dynamically include the `ApplicationProfile` context.
- [ ] Refactor `CloudWatchIngestionService` to be an optional adapter enabled by the profile.

## Acceptance Criteria
- Changing the `app.profile.name` in config changes the AI's diagnosis tone and context.
- The system can be "pointed" at a different SaaS app without changing core Kotlin code.
- AI system prompt includes: "You are an expert analyst for [App Name], which uses [Stack]..."
