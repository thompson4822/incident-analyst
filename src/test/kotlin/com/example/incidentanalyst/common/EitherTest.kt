package com.example.incidentanalyst.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EitherTest {

    @Test
    fun `map should transform right value`() {
        val either: Either<String, Int> = Either.Right(10)
        val result = either.map { it * 2 }
        
        assertTrue(result is Either.Right)
        assertEquals(20, (result as Either.Right).value)
    }

    @Test
    fun `map should not transform left value`() {
        val either: Either<String, Int> = Either.Left("error")
        val result = either.map { it * 2 }
        
        assertTrue(result is Either.Left)
        assertEquals("error", (result as Either.Left).value)
    }

    @Test
    fun `flatMap should transform right value to new either`() {
        val either: Either<String, Int> = Either.Right(10)
        val result = either.flatMap { Either.Right(it * 2) }
        
        assertTrue(result is Either.Right)
        assertEquals(20, (result as Either.Right).value)
    }

    @Test
    fun `flatMap should return left if transformation returns left`() {
        val either: Either<String, Int> = Either.Right(10)
        val result = either.flatMap { Either.Left("failed") }
        
        assertTrue(result is Either.Left)
        assertEquals("failed", (result as Either.Left).value)
    }

    @Test
    fun `fold should return result of ifRight for right value`() {
        val either: Either<String, Int> = Either.Right(10)
        val result = either.fold(
            ifLeft = { "left: $it" },
            ifRight = { "right: $it" }
        )
        
        assertEquals("right: 10", result)
    }

    @Test
    fun `fold should return result of ifLeft for left value`() {
        val either: Either<String, Int> = Either.Left("error")
        val result = either.fold(
            ifLeft = { "left: $it" },
            ifRight = { "right: $it" }
        )
        
        assertEquals("left: error", result)
    }
}
