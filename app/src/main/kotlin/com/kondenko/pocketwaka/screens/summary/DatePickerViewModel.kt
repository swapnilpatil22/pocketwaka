package com.kondenko.pocketwaka.screens.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.kizitonwose.calendarview.model.CalendarDay
import com.kondenko.pocketwaka.data.android.ConnectivityStatusProvider
import com.kondenko.pocketwaka.data.android.HumanReadableDateFormatter
import com.kondenko.pocketwaka.domain.summary.model.AvailableRange
import com.kondenko.pocketwaka.domain.summary.usecase.GetAvailableRange
import com.kondenko.pocketwaka.screens.base.BaseViewModel
import com.kondenko.pocketwaka.screens.summary.FragmentDatePicker.DaySelectionState
import com.kondenko.pocketwaka.utils.WakaLog
import com.kondenko.pocketwaka.utils.date.DateProvider
import com.kondenko.pocketwaka.utils.date.DateRange
import com.kondenko.pocketwaka.utils.extensions.notNull
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.threeten.bp.LocalDate

class DatePickerViewModel(
      private val dateFormatter: HumanReadableDateFormatter,
      private val connectivityStatusProvider: ConnectivityStatusProvider,
      dateProvider: DateProvider,
      getAvailableRange: GetAvailableRange
) : BaseViewModel<Nothing>() {

    // Events

    private val availableRange = MutableLiveData<AvailableRange>()

    private val selectedRange = MutableLiveData<SummaryRangeState>().apply {
        distinctUntilChanged()
    }

    private val titles = MutableLiveData<String>()

    private val calendarInvalidationEvents = MutableLiveData<Unit>()

    private val closeEvents = MutableLiveData<Unit>()

    // Dates

    private val today = DateRange.SingleDay(dateProvider.today)

    private var startDate: LocalDate? = null

    private var endDate: LocalDate? = null

    private val adjacentDatesNumber = 2

    // Properties

    private val selectedDateRange: DateRange?
        get() = when {
            startDate != null && endDate != null -> DateRange.Range(startDate!!, endDate!!)
            startDate != null -> DateRange.SingleDay(startDate!!)
            else -> null
        }

    val isStatsRangeUnlimited: Boolean
        get() = availableRange.value == AvailableRange.Unlimited

    /**
     * Which month the calendar should show when the date picker is opened
     */
    val dateToScrollTo
        get() = endDate ?: startDate

    init {
        disposables +=
              connectivityStatusProvider.isNetworkAvailable()
                    .concatMapSingle { getAvailableRange.build() }
                    .doOnNext { takeIf { availableRange.value == null }?.selectDate(today, true) }
                    .subscribeBy(onNext = availableRange::postValue, onError = WakaLog::e)
    }

    fun availableRangeChanges(): LiveData<AvailableRange> = availableRange

    fun dateChanges(): LiveData<SummaryRangeState> = selectedRange

    fun titleChanges(): LiveData<String> = titles

    fun dataSelectionEvents(): LiveData<DateRange?> = calendarInvalidationEvents.map { selectedDateRange }

    fun closeEvents(): LiveData<Unit> = closeEvents

    fun selectDate(dateRange: DateRange, invalidateScreens: Boolean) {
        titles.value = dateFormatter.format(dateRange)
        startDate = dateRange.start
        when (dateRange) {
            is DateRange.SingleDay -> {
                endDate = null
                dateRange.loadAdjacentDays(invalidateScreens)
            }
            is DateRange.Range -> {
                endDate = dateRange.end
                dateRange.loadRange(invalidateScreens)
            }
        }
        calendarInvalidationEvents.value = Unit
    }

    fun onDayClicked(day: CalendarDay) {
        val date = day.date
        if (startDate != null) {
            if (date < startDate || endDate != null) {
                startDate = date
                endDate = null
            } else if (date != startDate) {
                endDate = date
            } else {
                startDate = null
            }
        } else {
            startDate = date
        }
        calendarInvalidationEvents.value = Unit
    }

    fun confirmDateSelection() {
        val startDate = startDate
        val endDate = endDate
        if (startDate == null && endDate == null) return
        val selection = when {
            (startDate != null) xor (endDate != null) -> {
                DateRange.SingleDay(
                      startDate
                            ?: endDate
                            ?: throw IllegalArgumentException("One of dates is null")
                )
            }
            startDate != null && endDate != null -> {
                DateRange.Range(startDate, endDate)
            }
            else -> {
                throw IllegalArgumentException("Both start date and end date are null")
            }
        }
        selectDate(selection, true)
        calendarInvalidationEvents.value = Unit
    }

    fun getSelectionState(day: CalendarDay) = when {
        day.date == startDate -> {
            if (endDate != null) DaySelectionState.Start else DaySelectionState.Single
        }
        day.date == endDate -> {
            DaySelectionState.End
        }
        notNull(startDate, endDate) && day.date.isAfter(startDate) && day.date.isBefore(endDate) -> {
            DaySelectionState.Middle
        }
        else -> {
            DaySelectionState.Unselected
        }
    }

    private fun DateRange.Range.loadRange(invalidateScreens: Boolean) {
        selectedRange.value = SummaryRangeState(listOf(this), invalidateScreens)
    }

    private fun DateRange.SingleDay.loadAdjacentDays(invalidateScreens: Boolean) {
        val isTodaySelected = today.date == date
        val newDates = if (isTodaySelected) {
            (adjacentDatesNumber downTo 0L).map(date::minusDays)
        } else {
            val numberOfDaysOnEachSide = adjacentDatesNumber / 2
            val range = (1L..numberOfDaysOnEachSide)
            val daysOnLeftLimit = when (val range = availableRange.value) {
                is AvailableRange.Limited -> range.date.start
                else -> null
            }
            // Discard days that are out of the range available to free users (i.e. 2 weeks)
            val daysOnLeftSide = range.reversed()
                  .map(date::minusDays)
                  .takeLastWhile { daysOnLeftLimit == null || it == daysOnLeftLimit || it.isAfter(daysOnLeftLimit) }
            val daysOnRightSide = range.map(date::plusDays)
            daysOnLeftSide + listOf(date) + daysOnRightSide
        }
              .map { DateRange.SingleDay(it) }
              .let { SummaryRangeState(dates = it, invalidateScreens = invalidateScreens, openLastItem = isTodaySelected) }
        selectedRange.value = newDates
    }

}