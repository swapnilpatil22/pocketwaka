package com.kondenko.pocketwaka.screens.base.stateful

import android.os.Parcelable
import android.support.v4.app.Fragment
import io.reactivex.Observable

abstract class ModelFragment<M : Parcelable> : Fragment() {

    abstract fun subscribeToModelChanges(modelObservable: Observable<M>)

}
