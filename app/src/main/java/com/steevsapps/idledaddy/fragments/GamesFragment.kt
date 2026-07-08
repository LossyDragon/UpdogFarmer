package com.steevsapps.idledaddy.fragments

//class GamesFragment : Fragment() {
//    private lateinit var viewModel: GamesViewModel
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        viewModel = ViewModelProvider(this)[GamesViewModel::class.java]
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View = ComposeView(requireContext()).apply {
//        setContent {
//            GamesScreen(
//                viewModel = viewModel,
//                onMenuClick = { (activity as? MainActivity)?.openDrawer() },
//            )
//        }
//    }
//
//    override fun onPause() {
//        val selected = viewModel.uiState.value.selected
//        if (selected.isNotEmpty()) {
//            // Save idling session
//            writeLastSession(selected.toMutableList())
//        }
//        super.onPause()
//    }
//
//    /**
//     * Called by MainActivity when the currently idling games change
//     */
//    fun update(games: ArrayList<Game>) {
//        viewModel.setSelected(games)
//    }
//
//    companion object {
//        // FAB menu tabs
//        const val TAB_GAMES: Int = 0
//        const val TAB_LAST: Int = 1
//        const val TAB_BLACKLIST: Int = 2
//
//        fun newInstance(): GamesFragment = GamesFragment()
//    }
//}