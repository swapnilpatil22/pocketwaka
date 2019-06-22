package com.kondenko.pocketwaka.screens.base

import android.accounts.NetworkErrorException

sealed class State<out T>(open val data: T?) {

    data class Success<T>(override val data: T) : State<T>(data)

    data class Loading<T>(
            override val data: T? = null,
            val skeletonData: T,
            val isInterrupting: Boolean = true
    ) : State<T>(data)

    data class Offline<T>(override val data: T?, val lastUpdated: Long? = null) : State<T>(data)

    object Empty : State<Nothing>(null)

    sealed class Failure<out T>(
            override val data: T?,
            open val exception: Throwable? = null,
            open val isFatal: Boolean = false
    ) : State<T>(data) {

        data class NoNetwork<T>(
                override val data: T? = null,
                override val exception: Throwable? = NetworkErrorException("Device is offline"),
                override val isFatal: Boolean = false
        ) : Failure<T>(data, exception, isFatal)

        data class UnknownRange<T>(
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

fun <T> State.Failure<T>.copyFrom(data: T?, exception: Throwable? = this.exception): State.Failure<T> {
    return when (this) {
        is State.Failure.NoNetwork<T> -> this.copy(data, exception)
        is State.Failure.UnknownRange<T> -> this.copy(data)
        is State.Failure.Unknown<T> -> this.copy(data, exception)
    }
}