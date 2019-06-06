package com.kondenko.pocketwaka.data.stats.repository

import android.content.Context
import com.kondenko.pocketwaka.R
import com.kondenko.pocketwaka.data.android.DateFormatter
import com.kondenko.pocketwaka.data.stats.dao.StatsDao
import com.kondenko.pocketwaka.data.stats.dto.StatsDto
import com.kondenko.pocketwaka.data.stats.model.Stats
import com.kondenko.pocketwaka.data.stats.service.StatsService
import com.kondenko.pocketwaka.domain.stats.model.StatsItem
import com.kondenko.pocketwaka.domain.stats.model.StatsModel
import com.kondenko.pocketwaka.utils.ColorProvider
import com.kondenko.pocketwaka.utils.TimeProvider
import com.kondenko.pocketwaka.utils.notNull
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class StatsRepository(
        private val context: Context,
        private val service: StatsService,
        private val dao: StatsDao,
        private val colorProvider: ColorProvider,
        private val dateFormatter: DateFormatter,
        private val timeProvider: TimeProvider
) {

    private enum class StatsType {
        Editors, Languages, Projects, OperatingSystems
    }

    fun getStats(tokenHeader: String, range: String, onLoadedFromServer: ((StatsDto) -> Unit)? = null): Observable<StatsDto> {
        val cache = getStatsFromCache(range)
        val server = getStatsFromServer(tokenHeader, range)
        return cache.concatWith(server)
    }


    fun cacheStats(stats: StatsDto) = dao.cacheStats(stats)

    private fun getStatsFromCache(range: String): Observable<StatsDto> = dao.getCachedStats(range).toObservable()

    private fun getStatsFromServer(tokenHeader: String, range: String): Observable<StatsDto> =
            service.getCurrentUserStats(tokenHeader, range)
                    .flatMapObservable {
                        if (it.stats != null) Observable.just(toDomainModel(range, it.stats, timeProvider.getCurrentTimeMillis()))
                        else Observable.error(NullPointerException("Stats are null"))
                    }

    protected fun toDomainModel(range: String, stats: Stats, dateUpdated: Long): StatsDto {
        operator fun MutableList<StatsModel>.plusAssign(item: StatsModel?) {
            item?.let(this::add)
        }

        val list = arrayListOf<StatsModel>(
                StatsModel.Info(
                        stats.dailyAverage?.toLong()?.secondsToHumanReadableTime(),
                        stats.totalSeconds?.roundToLong()?.secondsToHumanReadableTime()
                )
        )

        list += stats.convertBestDay(stats.dailyAverage)
        list += stats.projects?.toDomainModel(StatsType.Projects)
        list += stats.languages?.toDomainModel(StatsType.Languages)
        list += stats.editors?.toDomainModel(StatsType.Editors)
        list += stats.operatingSystems?.toDomainModel(StatsType.OperatingSystems)

        list += StatsModel.Metadata(
                range = range,
                lastUpdated = dateUpdated,
                isEmpty = stats.totalSeconds == 0.0
        )

        return StatsDto(range, timeProvider.getCurrentTimeMillis(), list)
    }

    private fun Stats.convertBestDay(dailyAverageSec: Int?): StatsModel.BestDay? = bestDay?.let { bestDay ->
        val date = bestDay.date?.let(dateFormatter::reformatBestDayDate)
        val timeSec = bestDay.totalSeconds?.roundToLong()
        val timeHumanReadable = timeSec?.secondsToHumanReadableTime()
        val percentAboveAverage = calculatePercentAboveAverage(timeSec, dailyAverageSec)
        if (notNull(date, timeHumanReadable, percentAboveAverage)) {
            StatsModel.BestDay(date!!, timeHumanReadable!!, percentAboveAverage!!)
        } else {
            null
        }
    }

    private fun calculatePercentAboveAverage(bestDayTotalSec: Long?, dailyAverageSec: Int?): Int? {
        if (bestDayTotalSec == null || dailyAverageSec == null) return null
        return (bestDayTotalSec * 100 / dailyAverageSec - 100).toInt()
    }

    private fun List<com.kondenko.pocketwaka.data.stats.model.StatsItem>?.toDomainModel(statsType: StatsRepository.StatsType): StatsModel.Stats? {
        val items = this?.map { StatsItem(it.hours, it.minutes, it.name, it.percent) }
        return items
                ?.zip(colorProvider.provideColors(items)) { item, color -> item.copy(color = color) }
                ?.let { StatsModel.Stats(getCardTitle(statsType), it) }
    }

    private fun Long.secondsToHumanReadableTime(): String {
        val hours = TimeUnit.SECONDS.toHours(this)
        val minutes = TimeUnit.SECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
        val templateHours = getHoursTemplate(hours.toInt())
        val templateMinutes = getMinutesTemplate(minutes.toInt())
        val timeBuilder = StringBuilder()
        if (hours > 0) timeBuilder.append(templateHours.format(hours)).append(' ')
        if (minutes > 0) timeBuilder.append(templateMinutes.format(minutes))
        return timeBuilder.toString()
    }

    private fun getHoursTemplate(hours: Int): String {
        return context.resources.getQuantityString(R.plurals.stats_time_format_hours, hours)
    }

    private fun getMinutesTemplate(minutes: Int): String {
        return context.resources.getQuantityString(R.plurals.stats_time_format_minutes, minutes)
    }

    private fun getCardTitle(statsType: StatsType): String = when (statsType) {
        StatsType.Projects -> context.getString(R.string.stats_card_header_projects)
        StatsType.Editors -> context.getString(R.string.stats_card_header_editors)
        StatsType.Languages -> context.getString(R.string.stats_card_header_languages)
        StatsType.OperatingSystems -> context.getString(R.string.stats_card_header_operating_systems)
    }

}