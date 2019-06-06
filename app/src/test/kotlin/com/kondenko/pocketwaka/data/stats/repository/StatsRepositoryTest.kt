package com.kondenko.pocketwaka.data.stats.repository

import android.content.Context
import android.content.res.Resources
import com.kondenko.pocketwaka.data.android.DateFormatter
import com.kondenko.pocketwaka.data.stats.dao.StatsDao
import com.kondenko.pocketwaka.data.stats.dto.StatsDto
import com.kondenko.pocketwaka.data.stats.model.Stats
import com.kondenko.pocketwaka.data.stats.model.StatsServiceResponse
import com.kondenko.pocketwaka.data.stats.service.StatsService
import com.kondenko.pocketwaka.utils.ColorProvider
import com.kondenko.pocketwaka.utils.TimeProvider
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import java.util.concurrent.TimeoutException

class StatsRepositoryTest {

    private val context = mock<Context>()

    private val service = mock<StatsService>()

    private val dao = mock<StatsDao>()

    private val colorProvider = mock<ColorProvider>()

    private val dateFormatter = mock<DateFormatter>()

    private val timeProvider = mock<TimeProvider>()

    private val repository = StatsRepository(
            context,
            service,
            dao,
            colorProvider,
            dateFormatter,
            timeProvider
    )

    private val token = "token"

    private val range = "7_days"

    @Before
    fun setupContext() {
        val resources = mock<Resources>()
        whenever(context.resources).doReturn(resources)
        whenever(context.getString(anyInt())).doReturn("string")
        whenever(resources.getQuantityString(anyInt(), anyInt())).doReturn("quantity string")
    }

    @Test
    fun `should fetch stats from the database and from the server`() {
        val cachedStats = mock<StatsDto>()
        val newStatsResponse = mock<Stats>()
        whenever(dao.getCachedStats(range))
                .doReturn(Maybe.just(cachedStats))
        whenever(service.getCurrentUserStats(token, range))
                .doReturn(Single.just(StatsServiceResponse(newStatsResponse)))
        with(repository.getStats(token, range).test()) {
            assertNoErrors()
            assertValueCount(2)
            assertValueAt(0, cachedStats)
        }
    }

    @Test
    fun `should show stats from the database when the server returns an error`() {
        val cachedStats = mock<StatsDto>()
        val exception = TimeoutException("mock exception")
        whenever(dao.getCachedStats(range))
                .doReturn(Maybe.just(cachedStats))
        whenever(service.getCurrentUserStats(token, range))
                .doReturn(Single.error(exception))
        with(repository.getStats(token, range).test()) {
            assertValueCount(1)
            assertValue(cachedStats)
            assertError(exception)
        }
    }

    @Test
    fun `should only show stats from the server`() {
        val newStatsResponse = mock<Stats>()
        whenever(dao.getCachedStats(range))
                .doReturn(Maybe.empty())
        whenever(service.getCurrentUserStats(token, range))
                .doReturn(Single.just(StatsServiceResponse(newStatsResponse)))
        with(repository.getStats(token, range).test()) {
            assertNoErrors()
            assertValueCount(1)
        }
    }

}