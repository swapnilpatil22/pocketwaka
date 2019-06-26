package com.kondenko.pocketwaka.screens.stats


import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kondenko.pocketwaka.Const
import com.kondenko.pocketwaka.R
import com.kondenko.pocketwaka.domain.stats.model.StatsModel
import com.kondenko.pocketwaka.screens.StateFragment
import com.kondenko.pocketwaka.screens.base.State
import com.kondenko.pocketwaka.utils.extensions.adjustForDensity
import com.kondenko.pocketwaka.utils.report
import com.kondenko.pocketwaka.utils.transaction
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_stats.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class FragmentStatsTab : Fragment() {

    companion object {
        const val ARG_RANGE = "range"
    }

    private val vm: StatsViewModel by viewModel { parametersOf(arguments?.getString(ARG_RANGE)) }

    /**
     * The minimum value the RecyclerView has to be scrolled for
     * for the AppBar shadow to be shown
     */
    private val minScrollOffset: Float by lazy {
        val value = 8f
        context?.adjustForDensity(value) ?: value
    }

    private var shadowAnimationNeeded = true

    private val scrollDirection = BehaviorSubject.create<ScrollDirection>()

    private lateinit var skeletonAdapter: StatsAdapter

    private lateinit var statsAdapter: StatsAdapter

    private var fragmentState: StateFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        vm.state().observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Success -> it.render()
                is State.Loading -> it.render()
                is State.Offline -> it.render()
                is State.Empty -> it.render()
                is State.Failure -> it.render()
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        statsAdapter = StatsAdapter(context)
        skeletonAdapter = StatsAdapter(context, true)
    }

    private fun setupUi() {
        fragmentState = childFragmentManager.findFragmentById(R.id.fragment_state) as StateFragment
        with(recyclerview_stats) {
            layoutManager = LinearLayoutManager(context)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateAppBarElevation()
                }
            })
        }
        showData(true)
    }

    private fun State.Loading<List<StatsModel>>.render() {
        showData(true)
        if (isInterrupting) {
            skeletonAdapter.items = skeletonData
            recyclerview_stats.adapter = skeletonAdapter
        } else {
            recyclerview_stats.adapter = statsAdapter
            statsAdapter.items = listOf(StatsModel.Status.Loading()) + (data ?: emptyList())
        }
    }

    private fun State.Success<List<StatsModel>>.render() {
        showData(true)
        statsAdapter.items = data
        recyclerview_stats.adapter = statsAdapter
    }

    private fun State.Failure<List<StatsModel>>.render() {
        exception?.report()
        showData(!isFatal)
        if (isFatal) {
            fragmentState?.setState(this, vm::update)
        } else {
            data?.let { statsAdapter.items = it }
            view?.let {
                val errorRes = when (this) {
                    is State.Failure.Unknown -> R.string.stats_error_unknown
                    is State.Failure.UnknownRange -> R.string.stats_error_unknown_range
                    is State.Failure.NoNetwork -> R.string.stats_error_unknown_range_no_network
                }
                Snackbar.make(it, errorRes, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun State.Offline<List<StatsModel>>.render() {
        showData(data != null)
        if (data == null) {
            fragmentState?.setState(this)
        } else {
            recyclerview_stats.adapter = statsAdapter
            statsAdapter.items = listOf(StatsModel.Status.Offline()) + data
        }
    }

    private fun State.Empty.render() {
        showData(false)
        fragmentState?.setState(this, ::openPlugins)
    }

    private fun showData(show: Boolean) {
        recyclerview_stats.isVisible = show
        fragmentState?.let {
            childFragmentManager.transaction {
                if (show) hide(it)
                else show(it)
            }
        }
    }

    private fun openPlugins() {
        val uri = Const.URL_PLUGINS
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(ContextCompat.getColor(context!!, R.color.color_primary))
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(uri))
    }

    private fun updateAppBarElevation() {
        shadowAnimationNeeded = if (recyclerview_stats.computeVerticalScrollOffset() >= minScrollOffset) {
            if (shadowAnimationNeeded) scrollDirection.onNext(ScrollDirection.Down)
            false
        } else {
            scrollDirection.onNext(ScrollDirection.Up)
            true
        }
    }

    fun scrollDirection(): Observable<ScrollDirection> = scrollDirection

    fun subscribeToRefreshEvents(refreshEvents: Observable<Any>): Disposable {
        return refreshEvents.subscribe {
            vm.update()
        }
    }

}
