package com.example.incidentanalyst.rag

import com.pgvector.PGvector
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Query
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.ByteBuffer

@ApplicationScoped
class RunbookEmbeddingRepository : PanacheRepository<RunbookEmbeddingEntity> {

    @ConfigProperty(name = "quarkus.datasource.db-kind")
    lateinit var dbKind: String

    fun findSimilar(queryEmbedding: ByteArray, minScore: Double, limit: Int): List<SimilarRunbookResult> {
        if (dbKind == "h2") {
            return findSimilarInMemory(queryEmbedding, minScore, limit)
        }

        val sql = """
            SELECT 
                re.id,
                re.fragment_id,
                re.text,
                1 - (re.embedding <=> :embedding) AS similarity,
                re.source_type
            FROM runbook_embeddings re
            WHERE 1 - (re.embedding <=> :embedding) >= :minScore
            ORDER BY similarity DESC
            LIMIT :limit
        """.trimIndent()

        val query: Query = getEntityManager().createNativeQuery(sql)
        val pgVector = PGvector(byteArrayToFloatArray(queryEmbedding))
        query.setParameter("embedding", pgVector)
        query.setParameter("minScore", minScore)
        query.setParameter("limit", limit)

        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<Array<Any?>>

        return results.map { row ->
            SimilarRunbookResult(
                id = (row[0] as Number).toLong(),
                fragmentId = (row[1] as Number).toLong(),
                text = row[2] as String?,
                similarity = (row[3] as Number).toDouble(),
                sourceType = row[4] as String? ?: "OFFICIAL_RUNBOOK"
            )
        }
    }

    private fun findSimilarInMemory(
        queryEmbedding: ByteArray,
        minScore: Double,
        limit: Int
    ): List<SimilarRunbookResult> {
        val queryVector = byteArrayToFloatArray(queryEmbedding)

        return listAll().mapNotNull { entity ->
            val fragmentId = entity.fragment?.id ?: return@mapNotNull null
            val similarity = cosineSimilarity(queryVector, byteArrayToFloatArray(entity.embedding))
            if (similarity >= minScore) {
                SimilarRunbookResult(
                    id = requireNotNull(entity.id),
                    fragmentId = fragmentId,
                    text = entity.text,
                    similarity = similarity,
                    sourceType = entity.sourceType
                )
            } else {
                null
            }
        }.sortedByDescending { it.similarity }
            .take(limit)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val size = minOf(a.size, b.size)
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in 0 until size) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0.0 || normB == 0.0) 0.0 else dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }

    private fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val floatArray = FloatArray(byteArray.size / 4)
        val buffer = ByteBuffer.wrap(byteArray)
        for (i in floatArray.indices) {
            floatArray[i] = buffer.float
        }
        return floatArray
    }
}
