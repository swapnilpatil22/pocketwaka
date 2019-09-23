package com.kondenko.pocketwaka.domain.daily.usecase

import com.kondenko.pocketwaka.data.android.ConnectivityStatusProvider
import com.kondenko.pocketwaka.data.summary.model.database.SummaryDbModel
import com.kondenko.pocketwaka.domain.StatefulUseCase
import com.kondenko.pocketwaka.domain.daily.model.SummaryUiModel
import com.kondenko.pocketwaka.screens.State
import com.kondenko.pocketwaka.screens.daily.SummaryState
import com.kondenko.pocketwaka.utils.SchedulersContainer

class GetSummaryState(
        schedulers: SchedulersContainer,
        getSummary: GetSummary,
        connectivityStatusProvider: ConnectivityStatusProvider
) : StatefulUseCase<GetSummary.Params, List<SummaryUiModel>, SummaryDbModel>(
        schedulers = schedulers,
        useCase = getSummary,
        connectivityStatusProvider = connectivityStatusProvider
) {

    override fun databaseModelToState(model: SummaryDbModel, isConnected: Boolean): State<List<SummaryUiModel>> {
        return when {
            model.isEmpty == true -> SummaryState.EmptyRange
            model.isAccountEmpty == true -> State.Empty
            else -> super.databaseModelToState(model, isConnected)
        }
    }

}