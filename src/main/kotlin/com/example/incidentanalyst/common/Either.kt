package com.example.incidentanalyst.common

/**
 * A functional data type that represents a value of one of two possible types.
 * An instance of Either is an instance of either [Left] or [Right].
 *
 * Conventionally, [Left] is used for failure and [Right] is used for success.
 */
sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    inline fun <R2> map(f: (R) -> R2): Either<L, R2> =
        when (this) {
            is Left -> this
            is Right -> Right(f(value))
        }

    inline fun <L2> mapLeft(f: (L) -> L2): Either<L2, R> =
        when (this) {
            is Left -> Left(f(value))
            is Right -> this
        }

    inline fun <R2> flatMap(f: (R) -> Either<@UnsafeVariance L, R2>): Either<L, R2> =
        when (this) {
            is Left -> this
            is Right -> f(value)
        }

    inline fun <T> fold(ifLeft: (L) -> T, ifRight: (R) -> T): T =
        when (this) {
            is Left -> ifLeft(value)
            is Right -> ifRight(value)
        }

    fun isRight(): Boolean = this is Right
    fun isLeft(): Boolean = this is Left

    fun getOrNull(): R? = when (this) {
        is Right -> value
        is Left -> null
    }

    fun leftOrNull(): L? = when (this) {
        is Left -> value
        is Right -> null
    }
}

/**
 * Executes the given [action] if this is a [Either.Right].
 */
inline fun <L, R> Either<L, R>.onRight(action: (R) -> Unit): Either<L, R> {
    if (this is Either.Right) action(value)
    return this
}

/**
 * Executes the given [action] if this is a [Either.Left].
 */
inline fun <L, R> Either<L, R>.onLeft(action: (L) -> Unit): Either<L, R> {
    if (this is Either.Left) action(value)
    return this
}
