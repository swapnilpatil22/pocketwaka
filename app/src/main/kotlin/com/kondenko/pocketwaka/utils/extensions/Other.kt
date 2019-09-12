package com.kondenko.pocketwaka.utils.extensions

import android.content.SharedPreferences
import android.graphics.Matrix
import android.graphics.Path
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.crashlytics.android.Crashlytics
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import timber.log.Timber

fun notNull(vararg values: Any?): Boolean = values.all { it != null }

fun Float.negateIfTrue(condition: Boolean) = if (condition) -this else this

fun <T> T?.singleOrErrorIfNull(exception: Throwable): Single<T> = this?.let { Single.just(it) }
        ?: Single.error(exception)

inline fun SharedPreferences.edit(crossinline action: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    editor.action()
    editor.apply()
}

inline fun FragmentManager.transaction(crossinline action: androidx.fragment.app.FragmentTransaction.() -> androidx.fragment.app.FragmentTransaction) {
    this.beginTransaction().action().commit()
}

/**
 * Prints log output and sends a report to crashlyics about the given exception
 */
fun Throwable.report(message: String? = null) {
    Timber.e(this, message ?: this.message)
    Crashlytics.logException(this)
}

fun createPath(build: Path.() -> Unit): Path = Path().apply {
    build()
    close()
}

fun Path.applyMatrix(actions: Matrix.() -> Unit) = Matrix().also { matrix ->
    matrix.actions()
    transform(matrix)
}

operator fun <T : Comparable<T>> ClosedRange<T>.component1() = this.start

operator fun <T : Comparable<T>> ClosedRange<T>.component2() = this.endInclusive

operator fun <T> List<T>.times(times: Int): List<T> {
    val list = this.toMutableList()
    for (i in (1 until times)) {
        list.addAll(this)
    }
    return list
}

fun Disposable?.attachToLifecycle(lifecycle: LifecycleOwner) {
    lifecycle.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            this@attachToLifecycle?.dispose()
        }
    })
}

fun SharedPreferences.getStringOrThrow(key: String) =
        getString(key, null) ?: throw NullPointerException("Preference with key $key not found")