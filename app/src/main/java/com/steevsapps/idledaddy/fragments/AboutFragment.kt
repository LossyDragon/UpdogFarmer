package com.steevsapps.idledaddy.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.steevsapps.idledaddy.ui.screen.about.AboutScreen

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AboutScreen(onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() })
        }
    }

    companion object {
        val TAG: String = AboutFragment::class.java.getSimpleName()
        fun newInstance(): AboutFragment = AboutFragment()
    }
}