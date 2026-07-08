package com.steevsapps.idledaddy.fragments

//class HomeFragment : Fragment() {
//    private lateinit var viewModel: HomeViewModel
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View = ComposeView(requireContext()).apply {
//        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
//        setContent {
//            HomeScreen(
//                viewModel = viewModel,
//                onMenuClick = { (activity as? MainActivity)?.openDrawer() },
//                onLoginClick = { startActivity(LoginActivity.createIntent(requireContext())) },
//                onStopSteam = { (activity as? MainActivity)?.stopSteamService() },
//            )
//        }
//    }
//
//    companion object {
//        fun newInstance(): HomeFragment = HomeFragment()
//    }
//}