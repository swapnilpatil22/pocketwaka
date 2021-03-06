package com.kondenko.pocketwaka.screens

import android.accounts.NetworkErrorException

open class State<out T>(open val data: T?) {

    data class Success<T>(override val data: T) : State<T>(data)

    data class Loading<T>(
          override val data: T? = null,
          val isInterrupting: Boolean = true
    ) : State<T>(data)

    data class Offline<T>(override val data: T?, val lastUpdated: Long? = null) : State<T>(data)

    object Empty : State<Nothing>(null)

    sealed class Failure<out T>(
          override val data: T?,
          open val exception: Throwable? = null,
          open val isFatal: Boolean = false
    ) : State<T>(data) {

        data class Unauthorized(override val exception: Throwable?) : Failure<Nothing>(null, exception, true)

        data class NoNetwork<T>(
              override val data: T? = null,
              override val exception: Throwable? = NetworkErrorException("Device is offline"),
              override val isFatal: Boolean = false
        ) : Failure<T>(data, exception, isFatal)

        data class InvalidParams<T>(
              override val data: T? = null,
              override val isFatal: Boolean = false
        ) : Failure<T>(data, IllegalArgumentException("Unknown range"), isFatal)

        data class Unknown<T>(
              override val data: T? = null,
              override val exception: Throwable? = null,
              override val isFatal: Boolean = false
        ) : Failure<T>(data, exception, isFatal)

    }

}

fun <T> State<T>.copyWithData(data: T): State<T>? = when (this) {
    is State.Success -> copy(data = data)
    is State.Loading -> copy(data = data)
    is State.Offline -> copy(data = data)
    is State.Failure.NoNetwork -> copy(data = data)
    is State.Failure.InvalidParams -> copy(data = data)
    is State.Failure.Unknown -> copy(data = data)
    else -> null
}