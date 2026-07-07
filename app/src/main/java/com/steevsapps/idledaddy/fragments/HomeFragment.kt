package com.steevsapps.idledaddy.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.steevsapps.idledaddy.LoginActivity
import com.steevsapps.idledaddy.MainActivity
import com.steevsapps.idledaddy.ui.screen.home.HomeScreen
import com.steevsapps.idledaddy.ui.screen.home.HomeViewModel

class HomeFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            HomeScreen(
                viewModel = viewModel,
                onMenuClick = { (activity as? MainActivity)?.openDrawer() },
                onLoginClick = { startActivity(LoginActivity.createIntent(requireContext())) },
                onStopSteam = { (activity as? MainActivity)?.stopSteamService() },
            )
        }
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }
}