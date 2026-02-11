package com.example.incidentanalyst.rag

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.query.Metadata
import jakarta.enterprise.context.ApplicationScoped
import java.util.function.Supplier

@ApplicationScoped
class IncidentRetrievalAugmentorSupplier(
    private val incidentRetriever: ContentRetriever
) : Supplier<RetrievalAugmentor> {

    override fun get(): RetrievalAugmentor {
        return DefaultRetrievalAugmentor.builder()
            .contentRetriever(incidentRetriever)
            .build()
    }
}
