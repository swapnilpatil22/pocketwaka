package com.kondenko.pocketwaka.screens.fragments.states


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.kondenko.pocketwaka.R
import com.kondenko.pocketwaka.api.model.stats.DataWrapper

class FragmentLoadingState : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_all_state_loading, container, false)
    }

}
