package com.kondenko.pocketwaka.screens.daily

import com.kondenko.pocketwaka.domain.daily.model.SummaryUiModel
import com.kondenko.pocketwaka.screens.State

sealed class SummaryState(data: List<SummaryUiModel>?) : State<List<SummaryUiModel>>(data) {
    sealed class Empty : SummaryState(null) {
        object EmptyRange : Empty()
        object EmptyAccount : Empty()
    }
}