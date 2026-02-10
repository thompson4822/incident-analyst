package com.example.incidentanalyst.rag

import com.pgvector.PGvector
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Query
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.ByteBuffer

@ApplicationScoped
class IncidentEmbeddingRepository : PanacheRepository<IncidentEmbeddingEntity> {

    @ConfigProperty(name = "quarkus.datasource.db-kind")
    lateinit var dbKind: String

    fun findSimilar(queryEmbedding: ByteArray, minScore: Double, limit: Int): List<SimilarIncidentResult> {
        if (dbKind == "h2") {
            return findSimilarInMemory(queryEmbedding, minScore, limit)
        }

        val sql = """
            SELECT
                ie.id,
                ie.incident_id,
                ie.text,
                1 - (ie.embedding <=> :embedding) AS similarity,
                ie.source_type
            FROM incident_embeddings ie
            WHERE 1 - (ie.embedding <=> :embedding) >= :minScore
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
            SimilarIncidentResult(
                id = (row[0] as Number).toLong(),
                incidentId = (row[1] as Number).toLong(),
                text = row[2] as String?,
                similarity = (row[3] as Number).toDouble(),
                sourceType = row[4] as String? ?: "RAW_INCIDENT"
            )
        }
    }

    private fun findSimilarInMemory(
        queryEmbedding: ByteArray,
        minScore: Double,
        limit: Int
    ): List<SimilarIncidentResult> {
        val queryVector = byteArrayToFloatArray(queryEmbedding)

        return listAll().mapNotNull { entity ->
            val incidentId = entity.incident?.id ?: return@mapNotNull null
            val similarity = cosineSimilarity(queryVector, vectorStringToFloatArray(entity.embedding))
            if (similarity >= minScore) {
                SimilarIncidentResult(
                    id = requireNotNull(entity.id),
                    incidentId = incidentId,
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

    private fun vectorStringToFloatArray(vector: String): FloatArray {
        if (vector.isBlank()) return FloatArray(0)
        return vector.removePrefix("[").removeSuffix("]")
            .split(",")
            .map { it.trim().toFloat() }
            .toFloatArray()
    }
}
