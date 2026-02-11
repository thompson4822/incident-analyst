package com.example.incidentanalyst.rag

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.quarkus.arc.profile.IfBuildProfile
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces

@ApplicationScoped
class EmbeddingStoreConfiguration {

    @Produces
    @IfBuildProfile("test")
    @ApplicationScoped
    fun inMemoryEmbeddingStore(): EmbeddingStore<TextSegment> {
        return InMemoryEmbeddingStore<TextSegment>()
    }
}
