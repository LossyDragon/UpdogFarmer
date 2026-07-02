package com.steevsapps.idledaddy.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.steam.model.Game
import kotlin.math.ceil

class HomeFragment : Fragment() {
    // Status message
    private lateinit var status: View
    private lateinit var statusImg: ImageView
    private lateinit var statusText: TextView
    private lateinit var dropInfo: ViewGroup
    private lateinit var cardCountText: TextView
    private lateinit var gameCountText: TextView

    private lateinit var startIdling: TextView

    private lateinit var nowPlaying: ViewGroup
    private lateinit var game: TextView
    private lateinit var gameIcon: ImageView
    private lateinit var cardDropsRemaining: TextView
    private lateinit var pauseResumeButton: ImageView
    private lateinit var nextButton: ImageView

    private var loggedIn = false
    private var farming = false

    fun update(loggedIn: Boolean, farming: Boolean) {
        this.loggedIn = loggedIn
        this.farming = farming
        updateStatus()
    }

    /**
     * Show the drop info card
     */
    fun showDropInfo(gameCount: Int, cardCount: Int) {
        dropInfo.visibility = View.VISIBLE
        gameCountText.text = resources.getQuantityString(
            R.plurals.games_left,
            gameCount,
            gameCount
        )
        cardCountText.text = resources.getQuantityString(
            R.plurals.card_drops_remaining,
            cardCount,
            cardCount
        )
    }

    /**
     * Hide the drop info card
     */
    fun hideDropInfo() {
        dropInfo.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loggedIn = requireArguments().getBoolean(LOGGED_IN, false)
        farming = requireArguments().getBoolean(FARMING, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.home_fragment, container, false)
        status = view.findViewById(R.id.status)
        statusImg = view.findViewById(R.id.status_img)
        statusText = view.findViewById(R.id.status_text)
        dropInfo = view.findViewById(R.id.drop_info)
        cardCountText = view.findViewById(R.id.card_count)
        gameCountText = view.findViewById(R.id.game_count)
        startIdling = view.findViewById(R.id.start_idling)
        nowPlaying = view.findViewById(R.id.now_playing_card)
        game = view.findViewById(R.id.game)
        gameIcon = view.findViewById(R.id.icon)
        cardDropsRemaining = view.findViewById(R.id.card_drops_remaining)
        pauseResumeButton = view.findViewById(R.id.pause_resume_button)
        nextButton = view.findViewById(R.id.next_button)
        updateStatus()
        return view
    }

    private fun setStatus(online: Boolean) {
        status.isClickable = !online
        statusImg.setImageResource(if (online) R.drawable.ic_check_circle_white_48dp else R.drawable.ic_error_white_48dp)
        statusText.setText(if (online) R.string.logged_in else R.string.tap_to_login)
    }

    /**
     * show/hide the "Now Playing" card
     */
    fun showNowPlaying(games: MutableList<Game>, isFarming: Boolean, isPaused: Boolean) {
        if (games.isEmpty()) {
            // Hide the Now playing card
            nowPlaying.visibility = View.GONE
            return
        }
        // Currently just show the first game
        val g = games[0]
        if (!minimizeData()) {
            Glide.with(requireActivity()).load(g.iconUrl).into(gameIcon)
        } else {
            gameIcon.setImageResource(R.drawable.ic_image_white_48dp)
        }
        if (g.appId == 0) {
            // Non-Steam game
            game.text = getString(R.string.playing_non_steam_game, g.name)
        } else {
            game.text = g.name
        }
        if (g.dropsRemaining > 0) {
            // Show card drops remaining
            cardDropsRemaining.text = resources.getQuantityString(
                R.plurals.card_drops_remaining,
                g.dropsRemaining,
                g.dropsRemaining
            )
        } else {
            // No card drops. Show hours played instead
            val quantity = if (g.hoursPlayed < 1) 0 else ceil(g.hoursPlayed.toDouble()).toInt()
            cardDropsRemaining.text = resources.getQuantityString(
                R.plurals.hours_on_record,
                quantity,
                g.hoursPlayed
            )
        }
        // Show the pause or resume button depending on if we're paused or not.
        pauseResumeButton.setImageResource(if (isPaused) R.drawable.ic_action_play else R.drawable.ic_action_pause)
        // Hide the "next" when idling multiple
        nextButton.visibility = if (isFarming && games.size == 1) View.VISIBLE else View.GONE
        // Show the card
        nowPlaying.visibility = View.VISIBLE
    }

    private fun updateStatus() {
        setStatus(loggedIn)
        startIdling.isEnabled = loggedIn && !farming
    }

    companion object {
        const val LOGGED_IN: String = "LOGGED_IN"
        const val FARMING: String = "FARMING"

        fun newInstance(loggedIn: Boolean, farming: Boolean): HomeFragment {
            val fragment = HomeFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(LOGGED_IN, loggedIn)
                putBoolean(FARMING, farming)
            }
            return fragment
        }
    }
}