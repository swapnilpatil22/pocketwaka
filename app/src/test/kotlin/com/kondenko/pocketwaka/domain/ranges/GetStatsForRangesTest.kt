package com.kondenko.pocketwaka.domain.ranges

import com.kondenko.pocketwaka.data.ranges.repository.RangeStatsRepository
import com.kondenko.pocketwaka.domain.auth.GetTokenHeaderValue
import com.kondenko.pocketwaka.domain.ranges.usecase.GetStatsForRanges
import com.kondenko.pocketwaka.testutils.testSchedulers
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.rxkotlin.toSingle
import org.junit.Test


class GetStatsForRangesTest {

    private val getTokenHeader: GetTokenHeaderValue = mock()

    private val rangeStatsRepository: RangeStatsRepository = mock()

    private val useCase = GetStatsForRanges(testSchedulers, getTokenHeader, rangeStatsRepository)

    private val header = "foo"

    private val range = "bar"

    @Test
    fun `should fetch token first`() {
        whenever(getTokenHeader.build()).doReturn(header.toSingle())
        useCase.invoke(GetStatsForRanges.Params(range))
        inOrder(getTokenHeader, rangeStatsRepository) {
            verify(getTokenHeader).build()
            verify(rangeStatsRepository).getData(RangeStatsRepository.Params(header, range))
        }
    }

}
