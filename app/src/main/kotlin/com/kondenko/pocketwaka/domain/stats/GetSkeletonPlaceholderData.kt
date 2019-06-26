package com.kondenko.pocketwaka.domain.stats

import com.kondenko.pocketwaka.domain.UseCaseObservable
import com.kondenko.pocketwaka.domain.stats.model.StatsItem
import com.kondenko.pocketwaka.domain.stats.model.StatsModel
import com.kondenko.pocketwaka.utils.SchedulersContainer
import com.kondenko.pocketwaka.utils.extensions.toObservable
import com.kondenko.pocketwaka.utils.times
import io.reactivex.Observable


class GetSkeletonPlaceholderData(schedulers: SchedulersContainer) : UseCaseObservable<Nothing, List<StatsModel>>(schedulers) {

    private val skeletonStatsCard = mutableListOf(StatsItem("", null, null, null)) * 3

    override fun build(params: Nothing?): Observable<List<StatsModel>> = listOf(
            StatsModel.Info(null, null),
            StatsModel.BestDay("", "", 0),
            StatsModel.Stats("", skeletonStatsCard),
            StatsModel.Stats("", skeletonStatsCard)
    ).toObservable()

}