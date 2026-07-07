package com.steevsapps.idledaddy.steam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.MainActivity
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.listeners.AndroidLogListener
import com.steevsapps.idledaddy.preferences.PrefsManager.clearUser
import com.steevsapps.idledaddy.preferences.PrefsManager.getCellId
import com.steevsapps.idledaddy.preferences.PrefsManager.getGuardData
import com.steevsapps.idledaddy.preferences.PrefsManager.getHoursUntilDrops
import com.steevsapps.idledaddy.preferences.PrefsManager.getOffline
import com.steevsapps.idledaddy.preferences.PrefsManager.getRefreshToken
import com.steevsapps.idledaddy.preferences.PrefsManager.getUsername
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.preferences.PrefsManager.stayAwake
import com.steevsapps.idledaddy.preferences.PrefsManager.useCustomLoginId
import com.steevsapps.idledaddy.preferences.PrefsManager.writeCellId
import com.steevsapps.idledaddy.preferences.PrefsManager.writeGuardData
import com.steevsapps.idledaddy.preferences.PrefsManager.writeRefreshToken
import com.steevsapps.idledaddy.preferences.PrefsManager.writeUsername
import com.steevsapps.idledaddy.steam.model.Game
import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPurchaseResultDetail
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2.CMsgClientRegisterKey
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.CredentialsAuthSession
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.FreeLicenseCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PurchaseResponseCallback
import `in`.dragonbra.javasteam.steam.handlers.steamauthticket.SteamAuthTicket
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamcontent.SteamContent
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steammatchmaking.SteamMatchmaking
import `in`.dragonbra.javasteam.steam.handlers.steamnetworking.SteamNetworking
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.ItemAnnouncementsCallback
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.AccountInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.types.GameID
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.NetHelpers.getIPAddress
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.javasteam.util.log.LogManager.addListener
import java.io.Closeable
import java.io.File
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32
import kotlin.concurrent.Volatile

class SteamService : Service() {
    private lateinit var steamClient: SteamClient
    private lateinit var manager: CallbackManager
    private lateinit var steamUser: SteamUser
    private lateinit var steamFriends: SteamFriends
    private lateinit var steamApps: SteamApps
    private val webHandler: SteamWebHandler = SteamWebHandler.instance
    private var wakeLock: WakeLock? = null
    private val subscriptions: MutableList<Closeable> = mutableListOf()

    private var farmIndex = 0
    private var gamesToFarm: MutableList<Game>? = null

    var currentGames: MutableList<Game> = mutableListOf()
        private set
    var gameCount: Int = 0
        private set
    var cardCount: Int = 0
        private set

    private var pendingAuthDetails: AuthSessionDetails? = null

    private var currentRefreshToken: String? = null

    @Volatile
    private var pendingQrLogin = false

    @Volatile
    private var pendingGuardCodeFuture: CompletableFuture<String>? = null

    @Volatile
    private var running = false // Service running

    @Volatile
    private var connected = false // Connected to Steam

    @Volatile
    var isFarming: Boolean = false // Currently farming
        private set

    @Volatile
    var isPaused: Boolean = false // Game paused
        private set

    @Volatile
    private var waiting = false // Waiting for user to stop playing

    @Volatile
    private var loginInProgress = true // Currently logging in, so don't reconnect on disconnects

    var steamId: Long = 0
        private set
    var isLoggedIn: Boolean = false
        private set
    var parentalStatus: Boolean = false
        private set

    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(8)
    private var farmHandle: ScheduledFuture<*>? = null
    private var waitHandle: ScheduledFuture<*>? = null

    private var keyToRedeem: String? = null
    private val pendingFreeLicenses = LinkedList<Int>()

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    inner class LocalBinder : Binder() {
        val service: SteamService
            get() = this@SteamService
    }

    /**
     * Login flow events for the currently bound login UI, delivered on the main thread.
     * Set through [LocalBinder]; only one listener at a time.
     */
    interface LoginEventListener {
        fun onLoginResult(result: EResult)
        fun onQrChallenge(url: String)
        fun onDeviceConfirmation()
    }

    var loginEventListener: LoginEventListener? = null

    private fun notifyLoginListener(block: (LoginEventListener) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            loginEventListener?.let(block)
        }
    }

    // This is the object that receives interactions from clients.
    private val binder: IBinder = LocalBinder()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                SKIP_INTENT -> skipGame()
                STOP_INTENT -> stopGame()
                PAUSE_INTENT -> pauseGame()
                RESUME_INTENT -> resumeGame()
            }
        }
    }

    private val farmTask = Runnable {
        try {
            farm()
        } catch (e: Exception) {
            Log.i(TAG, "FarmTask failed", e)
        }
    }

    /**
     * Wait for user to NOT be in-game so we can resume idling
     */
    private val waitTask: Runnable = Runnable {
        try {
            Log.i(TAG, "Checking if we can resume idling...")
            val notInGame = webHandler.checkIfNotInGame()
            if (notInGame == null) {
                Log.i(TAG, "Invalid cookie data or no internet, reconnecting...")
                steamClient.disconnect()
            } else if (notInGame) {
                Log.i(TAG, "Resuming...")
                waiting = false
                steamClient.disconnect()
                waitHandle?.cancel(false)
            }
        } catch (e: Exception) {
            Log.i(TAG, "WaitTask failed", e)
        }
    }

    fun startFarming() {
        if (!isFarming) {
            isFarming = true
            isPaused = false
            executor.execute(farmTask)
        }
    }

    fun stopFarming() {
        if (isFarming) {
            isFarming = false
            gamesToFarm = null
            farmIndex = 0
            currentGames.clear()
            unscheduleFarmTask()
        }
    }

    /**
     * Resume farming/idling
     */
    private fun resumeFarming() {
        if (isPaused || waiting) {
            return
        }

        if (isFarming) {
            Log.i(TAG, "Resume farming")
            executor.execute(farmTask)
        } else if (currentGames.size == 1) {
            Log.i(TAG, "Resume playing")
            Handler(Looper.getMainLooper()).post { idleSingle(currentGames[0]) }
        } else if (currentGames.size > 1) {
            Log.i(TAG, "Resume playing (multiple)")
            idleMultiple(currentGames)
        }
    }

    private fun farm() {
        if (isPaused || waiting) {
            return
        }
        Log.i(TAG, "Checking remaining card drops")
        var games: MutableList<Game>? = null
        for (i in 0..2) {
            games = webHandler.remainingGames
            if (games != null) {
                Log.i(TAG, "gotem")
                break
            }
            if (i + 1 < 3) {
                Log.i(TAG, "retrying...")
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return
                }
            }
        }

        if (games == null) {
            Log.i(TAG, "Invalid cookie data or no internet, reconnecting")
            steamClient.disconnect()
            return
        }
        gamesToFarm = games

        // Count the games and cards
        gameCount = games.size
        cardCount = games.sumOf { it.dropsRemaining }

        // Send farm event
        val event = Intent(FARM_EVENT)
        event.putExtra(GAME_COUNT, gameCount)
        event.putExtra(CARD_COUNT, cardCount)
        LocalBroadcastManager.getInstance(this).sendBroadcast(event)

        if (games.isEmpty()) {
            Log.i(TAG, "Finished idling")
            stopPlaying()
            updateNotification(getString(R.string.idling_finished))
            stopFarming()
            return
        }

        // Sort by hours played descending
        games.sortDescending()

        if (farmIndex >= games.size) {
            farmIndex = 0
        }
        val game = games[farmIndex]

        // TODO: Steam only updates play time every half hour, so maybe we should keep track of it ourselves
        if (game.hoursPlayed >= getHoursUntilDrops() || games.size == 1 || farmIndex > 0) {
            // Idle a single game
            Handler(Looper.getMainLooper()).post { idleSingle(game) }
            unscheduleFarmTask()
        } else {
            // Idle multiple games (max 32) until one has reached 2 hrs
            idleMultiple(games)
            scheduleFarmTask()
        }
    }

    fun skipGame() {
        val games = gamesToFarm
        if (games == null || games.size < 2) {
            return
        }

        farmIndex++
        if (farmIndex >= games.size) {
            farmIndex = 0
        }

        idleSingle(games[farmIndex])
    }

    fun stopGame() {
        isPaused = false
        stopPlaying()
        stopFarming()
        updateNotification(getString(R.string.stopped))
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(STOP_EVENT))
    }

    fun pauseGame() {
        isPaused = true
        stopPlaying()
        showPausedNotification()
        // Tell the activity to update
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(NOW_PLAYING_EVENT))
    }

    fun resumeGame() {
        if (currentGames.size == 1) {
            Log.i(TAG, "Resume playing")
            idleSingle(currentGames[0])
        } else if (currentGames.size > 1) {
            Log.i(TAG, "Resume playing (multiple)")
            idleMultiple(currentGames)
        } else if (isFarming) {
            Log.i(TAG, "Resume farming")
            isPaused = false
            executor.execute(farmTask)
        }
    }

    private fun scheduleFarmTask() {
        val handle = farmHandle
        if (handle == null || handle.isCancelled) {
            Log.i(TAG, "Starting farmtask")
            farmHandle = scheduler.scheduleWithFixedDelay(farmTask, 10, 10, TimeUnit.MINUTES)
        }
    }

    private fun unscheduleFarmTask() {
        val handle = farmHandle
        if (handle != null) {
            Log.i(TAG, "Stopping farmtask")
            handle.cancel(true)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        Log.i(TAG, "Service created")
        super.onCreate()

        val cellId = getCellId()
        val servers = File(filesDir, "servers.bin")
        val fileServerListProvider = FileServerListProvider(servers)

        val config = SteamConfiguration.create {
            it.withServerListProvider(fileServerListProvider)
            if (cellId >= 0) it.withCellID(cellId)
        }
        steamClient = SteamClient(config)

        steamUser = requireNotNull(steamClient.getHandler<SteamUser>())
        steamFriends = requireNotNull(steamClient.getHandler<SteamFriends>())
        steamApps = requireNotNull(steamClient.getHandler<SteamApps>())

        // Subscribe to callbacks
        manager = CallbackManager(steamClient)
        subscriptions.add(manager.subscribe<ConnectedCallback>(::onConnected))
        subscriptions.add(manager.subscribe<DisconnectedCallback>(::onDisconnected))
        subscriptions.add(manager.subscribe<LoggedOffCallback>(::onLoggedOff))
        subscriptions.add(manager.subscribe<LoggedOnCallback>(::onLoggedOn))
        subscriptions.add(manager.subscribe<PersonaStateCallback>(::onPersonaState))
        subscriptions.add(manager.subscribe<FreeLicenseCallback>(::onFreeLicense))
        subscriptions.add(manager.subscribe<AccountInfoCallback>(::onAccountInfo))
        subscriptions.add(manager.subscribe<ItemAnnouncementsCallback>(::onItemAnnouncements))
        subscriptions.add(manager.subscribe<PurchaseResponseCallback>(::onPurchaseResponse))

        // Unregister handlers we have no use for.
        steamClient.removeHandler<SteamGameCoordinator>()
        steamClient.removeHandler<SteamGameServer>()
        steamClient.removeHandler<SteamUserStats>()
        steamClient.removeHandler<SteamMasterServer>()
        steamClient.removeHandler<SteamCloud>()
        steamClient.removeHandler<SteamWorkshop>()
        steamClient.removeHandler<SteamScreenshots>()
        steamClient.removeHandler<SteamMatchmaking>()
        steamClient.removeHandler<SteamNetworking>()
        steamClient.removeHandler<SteamContent>()
        steamClient.removeHandler<SteamAuthTicket>()
        steamClient.removeHandler<SteamNotifications>()

        if (stayAwake()) {
            acquireWakeLock()
        }

        createChannel()

        if (BuildConfig.DEBUG) {
            addListener(AndroidLogListener())
        }

        startForeground(NOTIF_ID, buildNotification(getString(R.string.service_started)))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!running) {
            Log.i(TAG, "Command starting")
            val filter = IntentFilter().apply {
                addAction(SKIP_INTENT)
                addAction(STOP_INTENT)
                addAction(PAUSE_INTENT)
                addAction(RESUME_INTENT)
            }
            ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            start()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        Thread {
            steamUser.logOff()
            steamClient.disconnect()
        }.start()
        stopForeground(STOP_FOREGROUND_REMOVE)
        running = false
        stopFarming()
        executor.shutdownNow()
        scheduler.shutdownNow()
        releaseWakeLock()
        unregisterReceiver(receiver)

        subscriptions.forEach { it.close() }

        super.onDestroy()
    }

    /**
     * Create notification channel for Android O
     */
    private fun createChannel() {
        val name: CharSequence = getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.enableVibration(false)
        channel.enableLights(false)
        channel.setBypassDnd(false)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun changeStatus(status: EPersonaState) {
        if (isLoggedIn) {
            executor.execute { steamFriends.setPersonaState(status) }
        }
    }

    /**
     * Acquire WakeLock to keep the CPU from sleeping
     */
    fun acquireWakeLock() {
        if (wakeLock == null) {
            Log.i(TAG, "Acquiring WakeLock")
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
            lock.acquire(60 * 60 * 1000L) // 60 Minutes
            wakeLock = lock
        }
    }

    /**
     * Release the WakeLock
     */
    fun releaseWakeLock() {
        val lock = wakeLock
        if (lock != null) {
            Log.i(TAG, "Releasing WakeLock")
            lock.release()
            wakeLock = null
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Show idling notification
     * @param game the [Game] object
     */
    private fun showIdleNotification(game: Game) {
        Log.i(TAG, "Idle notification")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        @Suppress("DEPRECATION")
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                getString(
                    R.string.now_playing2,
                    if (game.appId == 0) getString(
                        R.string.playing_non_steam_game,
                        game.name
                    ) else game.name
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setStyle(mediaStyle)

        if (game.dropsRemaining > 0) {
            // Show drops remaining
            builder.setSubText(
                resources.getQuantityString(
                    R.plurals.card_drops_remaining,
                    game.dropsRemaining,
                    game.dropsRemaining
                )
            )
        }

        // Add the stop and pause actions
        val stopIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(STOP_INTENT).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val pauseIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(PAUSE_INTENT).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        builder.addAction(R.drawable.ic_action_stop, getString(R.string.stop), stopIntent)
        builder.addAction(R.drawable.ic_action_pause, getString(R.string.pause), pauseIntent)

        if (isFarming) {
            // Add the skip action
            val skipIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(SKIP_INTENT).setPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
            builder.addAction(R.drawable.ic_action_skip, getString(R.string.skip), skipIntent)
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!minimizeData()) {
            // Load game icon into notification
            Glide.with(applicationContext)
                .asBitmap()
                .load(game.iconUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        builder.setLargeIcon(resource)
                        nm.notify(NOTIF_ID, builder.build())
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        nm.notify(NOTIF_ID, builder.build())
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        } else {
            nm.notify(NOTIF_ID, builder.build())
        }
    }

    /**
     * Show "Big Text" style notification with the games we're idling
     * @param msg the games
     */
    private fun showMultipleNotification(msg: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Add stop and pause actions
        val stopIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(STOP_INTENT).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val pauseIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(PAUSE_INTENT).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(msg)
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.idling_multiple))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_action_stop, getString(R.string.stop), stopIntent)
            .addAction(R.drawable.ic_action_pause, getString(R.string.pause), pauseIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    private fun showPausedNotification() {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val resumeIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(RESUME_INTENT).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.paused))
            .setContentIntent(pi)
            .addAction(R.drawable.ic_action_play, getString(R.string.resume), resumeIntent)
            .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    /**
     * Used to update the notification
     *
     * @param text the text to display
     */
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID, buildNotification(text))
    }

    private fun idleSingle(game: Game) {
        Log.i(TAG, "Now playing ${game.name}")
        isPaused = false
        currentGames.clear()
        currentGames.add(game)
        playGames(game)
        showIdleNotification(game)
    }

    private fun idleMultiple(games: MutableList<Game>) {
        Log.i(TAG, "Idling multiple")
        isPaused = false
        val gamesCopy: MutableList<Game> = ArrayList(games)
        currentGames.clear()

        val size = minOf(gamesCopy.size, 32)

        val msg = StringBuilder()
        for (i in 0..<size) {
            val game = gamesCopy[i]
            currentGames.add(game)
            if (game.appId == 0) {
                // Non-Steam game
                msg.append(getString(R.string.playing_non_steam_game, game.name))
            } else {
                msg.append(game.name)
            }
            if (i + 1 < size) {
                msg.append("\n")
            }
        }

        playGames(*currentGames.toTypedArray())
        showMultipleNotification(msg.toString())
    }

    fun addGame(game: Game) {
        stopFarming()
        if (currentGames.isEmpty()) {
            idleSingle(game)
        } else {
            currentGames.add(game)
            idleMultiple(currentGames)
        }
    }

    fun addGames(games: MutableList<Game>) {
        stopFarming()
        if (games.size == 1) {
            idleSingle(games[0])
        } else if (games.size > 1) {
            idleMultiple(games)
        } else {
            stopGame()
        }
    }

    fun removeGame(game: Game) {
        stopFarming()
        currentGames.remove(game)
        if (currentGames.size == 1) {
            idleSingle(currentGames[0])
        } else if (currentGames.size > 1) {
            idleMultiple(currentGames)
        } else {
            stopGame()
        }
    }

    fun start() {
        running = true
        if (getRefreshToken().isNotEmpty()) {
            // We can log in using saved credentials
            executor.execute { steamClient.connect() }
        }
        // Run the callback handler
        executor.execute {
            while (running) {
                try {
                    manager.runWaitCallbacks(1000L)
                } catch (e: Exception) {
                    Log.i(TAG, "update() failed", e)
                }
            }
        }
    }

    /**
     * Log in with a fresh username/password. Two-factor codes (if needed) are requested afterward
     * through [GuardCodeAuthenticator] and supplied via [.submitTwoFactorCode].
     */
    fun login(username: String, password: String) {
        Log.i(TAG, "logging in")
        loginInProgress = true

        val details = AuthSessionDetails().apply {
            this.username = username
            this.password = password
            guardData = getGuardData().takeIf { it.isNotEmpty() }
            persistentSession = true
            clientOSType = EOSType.AndroidUnknown
            deviceFriendlyName = FRIENDLY_NAME
            authenticator = GuardCodeAuthenticator()
        }

        pendingAuthDetails = details
        executor.execute { steamClient.connect() }
    }

    /**
     * Log in by scanning a QR code with the Steam Mobile App. No password/2FA code is needed;
     * the QR auth session emits [LoginEventListener.onQrChallenge] with the URL to render as soon
     * as it begins, then blocks until the user approves the prompt on their phone.
     */
    fun loginWithQr() {
        Log.i(TAG, "logging in via QR")
        loginInProgress = true
        if (connected) {
            // Already connected (e.g. left over from a prior login attempt) - connect() won't fire
            // another ConnectedCallback, so start the QR session directly or we'd never show a code.
            doQrLogin()
        } else {
            pendingQrLogin = true
            executor.execute { steamClient.connect() }
        }
    }

    /**
     * Supply the Steam Guard code requested by [GuardCodeAuthenticator] for the in-progress login.
     */
    fun submitTwoFactorCode(code: String) {
        pendingGuardCodeFuture?.complete(code)
    }

    fun logoff() {
        Log.i(TAG, "logging off")
        loginInProgress = true
        isLoggedIn = false
        steamId = 0
        pendingAuthDetails = null
        pendingQrLogin = false
        currentRefreshToken = null
        cancelPendingGuardCode()
        currentGames.clear()
        keyToRedeem = null
        pendingFreeLicenses.clear()
        stopFarming()
        executor.execute {
            steamUser.logOff()
            steamClient.disconnect()
        }
        clearUser()
        updateNotification(getString(R.string.logged_out))
    }

    /**
     * Redeem Steam key or activate free license
     */
    fun redeemKey(key: String) {
        if (!isLoggedIn && getRefreshToken().isNotEmpty()) {
            Log.i(TAG, "Will redeem key at login")
            keyToRedeem = key
            return
        }
        Log.i(TAG, "Redeeming key...")
        if (key.matches("\\d+".toRegex())) {
            // Request a free license
            try {
                val freeLicense = key.toInt()
                addFreeLicense(freeLicense)
            } catch (_: NumberFormatException) {
                showToast(getString(R.string.invalid_key))
            }
        } else {
            // Register product key
            registerProductKey(key)
        }
    }

    /**
     * Request a free license
     */
    private fun addFreeLicense(freeLicense: Int) {
        pendingFreeLicenses.add(freeLicense)
        executor.execute { steamApps.requestFreeLicense(freeLicense) }
    }

    /**
     * Register a product key
     */
    private fun registerProductKey(productKey: String) {
        val registerKey = ClientMsgProtobuf<CMsgClientRegisterKey.Builder>(
            CMsgClientRegisterKey::class.java,
            EMsg.ClientRegisterKey
        )
        registerKey.body.key = productKey
        executor.execute { steamClient.send(registerKey) }
    }

    /**
     * Begin a fresh credentials-based auth session (username/password + 2FA), then log on with the
     * resulting refresh token once it completes. Runs on a background thread since both
     * `beginAuthSessionViaCredentials` and `pollingWaitForResult` block until Steam
     * responds (possibly waiting on [.submitTwoFactorCode]).
     * Needs to happen as soon as we connect or else we'll get an error.
     */
    private fun doCredentialsLogin(details: AuthSessionDetails) {
        executor.execute {
            try {
                val authSession: CredentialsAuthSession = steamClient.authentication
                    .beginAuthSessionViaCredentials(details)
                    .get()
                val pollResult: AuthPollResult = authSession.pollingWaitForResult().get()

                pollResult.newGuardData?.let { writeGuardData(it) }
                performLogOn(pollResult.accountName, pollResult.refreshToken)
            } catch (e: Exception) {
                Log.i(TAG, "Credentials login failed", e)
                cancelPendingGuardCode()
                keyToRedeem = null
                steamClient.disconnect()
                sendLoginResult(resolveFailureResult(e))
            }
        }
    }

    /**
     * Begin a fresh QR auth session and log on with the resulting refresh token once the user
     * approves the prompt in the Steam Mobile App. Runs on a background thread since both
     * `beginAuthSessionViaQR` and `pollingWaitForResult` block until Steam responds.
     */
    private fun doQrLogin() {
        executor.execute {
            try {
                val details = AuthSessionDetails().apply {
                    clientOSType = EOSType.AndroidUnknown
                    deviceFriendlyName = FRIENDLY_NAME
                    persistentSession = true
                }

                val authSession: QrAuthSession = steamClient.authentication
                    .beginAuthSessionViaQR(details)
                    .get()
                authSession.challengeUrlChanged = IChallengeUrlChanged { session ->
                    session?.let { sendQrChallengeUrl(it.challengeUrl) }
                }
                sendQrChallengeUrl(authSession.challengeUrl)

                val pollResult: AuthPollResult = authSession.pollingWaitForResult().get()

                performLogOn(pollResult.accountName, pollResult.refreshToken)
            } catch (e: Exception) {
                Log.i(TAG, "QR login failed", e)
                keyToRedeem = null
                steamClient.disconnect()
                sendLoginResult(resolveFailureResult(e))
            }
        }
    }

    private fun sendQrChallengeUrl(url: String) {
        notifyLoginListener { it.onQrChallenge(url) }
    }

    /**
     * Log on to the Steam3 network using a refresh token, used both for a freshly completed
     * credentials login and for restoring a saved session.
     */
    private fun performLogOn(username: String, refreshToken: String) {
        currentRefreshToken = refreshToken
        // Always keep this current: the QR login flow never types a username for LoginActivity to save
        writeUsername(username)

        val details = LogOnDetails(
            username = username,
            accessToken = refreshToken,
            clientOSType = EOSType.AndroidUnknown,
            machineName = FRIENDLY_NAME,
            shouldRememberPassword = true
        )
        if (useCustomLoginId()) {
            val localIP = steamClient.localIP
            if (localIP != null) {
                details.loginID = getIPAddress(localIP) xor CUSTOM_OBFUSCATION_MASK
            }
        }
        steamUser.logOn(details)
    }

    /**
     * Log in using a saved refresh token
     */
    private fun attemptRestoreLogin() {
        val username = getUsername()
        val refreshToken = getRefreshToken()
        if (username.isEmpty() || refreshToken.isEmpty()) return
        Log.i(TAG, "Restoring login")
        performLogOn(username, refreshToken)
    }

    /**
     * Mint a fresh web access token from our refresh token and use it to authenticate on the Steam website.
     */
    private fun attemptWebAuthentication(clientSteamId: SteamID, refreshToken: String): Boolean {
        Log.i(TAG, "Attempting SteamWeb authentication")
        for (i in 0..2) {
            try {
                val tokens = steamClient.authentication
                    .generateAccessTokenForApp(clientSteamId, refreshToken, true)
                    .get()
                if (webHandler.authenticate(clientSteamId.convertToUInt64(), tokens.accessToken)) {
                    Log.i(TAG, "Authenticated!")
                    return true
                }
            } catch (e: Exception) {
                Log.i(TAG, "Failed to generate a web access token", e)
            }

            if (i + 1 < 3) {
                Log.i(TAG, "Retrying...")
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return false
                }
            }
        }
        return false
    }

    /**
     * Authenticator bridging JavaSteam's poll-based Steam Guard prompts to the login UI: each
     * callback parks a future in [.pendingGuardCodeFuture] and tells the login screen (via
     * [LoginEventListener.onLoginResult], reusing the EResults it already branches on) to show the
     * code field. The actual code arrives later through [.submitTwoFactorCode].
     */
    private inner class GuardCodeAuthenticator : IAuthenticator {
        override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> {
            Log.i(TAG, "IAuthenticator: Device Confirmation")
            return requestGuardCode(
                if (previousCodeWasIncorrect)
                    EResult.TwoFactorCodeMismatch
                else
                    EResult.AccountLoginDeniedNeedTwoFactor
            )
        }

        override fun getEmailCode(
            email: String?,
            previousCodeWasIncorrect: Boolean
        ): CompletableFuture<String> {
            Log.i(TAG, "IAuthenticator: Email Code from $email")
            return requestGuardCode(
                if (previousCodeWasIncorrect)
                    EResult.InvalidLoginAuthCode
                else
                    EResult.AccountLogonDenied
            )
        }

        override fun acceptDeviceConfirmation(): CompletableFuture<Boolean> {
            Log.i(TAG, "IAuthenticator: Device Confirmation")
            notifyLoginListener { it.onDeviceConfirmation() }
            return CompletableFuture.completedFuture(true)
        }
    }

    private fun requestGuardCode(signal: EResult): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        pendingGuardCodeFuture = future
        sendLoginResult(signal)
        return future
    }

    private fun cancelPendingGuardCode() {
        val future = pendingGuardCodeFuture
        pendingGuardCodeFuture = null
        future?.cancel(false)
    }

    private fun sendLoginResult(result: EResult) {
        notifyLoginListener { it.onLoginResult(result) }
        // MainActivity still watches LOGIN_EVENT to refresh its status display
        val intent = Intent(LOGIN_EVENT)
        intent.putExtra(RESULT, result)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun resolveFailureResult(e: Exception): EResult {
        val cause = (e as? ExecutionException)?.cause ?: e
        return (cause as? AuthenticationException)?.result ?: EResult.Fail
    }

    private fun registerApiKey() {
        Log.i(TAG, "Registering API key")
        val result = webHandler.updateApiKey()
        Log.i(TAG, "API key result: $result")
        when (result) {
            SteamWebHandler.ApiKeyState.REGISTERED -> {}
            SteamWebHandler.ApiKeyState.ACCESS_DENIED -> showToast(getString(R.string.apikey_access_denied))
            // Call updateApiKey once more to actually update it
            SteamWebHandler.ApiKeyState.UNREGISTERED -> webHandler.updateApiKey()
            SteamWebHandler.ApiKeyState.ERROR -> showToast(getString(R.string.apikey_register_failed))
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("unused")
    private fun onConnected(callback: ConnectedCallback) {
        Log.i(TAG, "Connected()")
        connected = true
        val authDetails = pendingAuthDetails
        if (pendingQrLogin) {
            pendingQrLogin = false
            doQrLogin()
        } else if (authDetails != null) {
            doCredentialsLogin(authDetails)
            pendingAuthDetails = null
        } else {
            attemptRestoreLogin()
        }
    }

    @Suppress("unused")
    private fun onDisconnected(callback: DisconnectedCallback) {
        Log.i(TAG, "Disconnected()")
        connected = false
        isLoggedIn = false

        if (!loginInProgress) {
            // Try to reconnect after a 5-second delay
            scheduler.schedule({
                Log.i(TAG, "Reconnecting")
                steamClient.connect()
            }, 5, TimeUnit.SECONDS)
        } else {
            // SteamKit may disconnect us while logging on (if already connected),
            // but since it reconnects immediately after we do not have to reconnect here.
            Log.i(TAG, "NOT reconnecting (logon in progress)")
        }

        // Tell the activity that we've been disconnected from Steam
        val intent = Intent(DISCONNECT_EVENT)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun onLoggedOff(callback: LoggedOffCallback) {
        Log.i(TAG, "Logoff result ${callback.result}")
        if (callback.result == EResult.LoggedInElsewhere) {
            updateNotification(getString(R.string.logged_in_elsewhere))
            unscheduleFarmTask()
            if (!waiting) {
                waiting = true
                waitHandle = scheduler.scheduleWithFixedDelay(waitTask, 0, 30, TimeUnit.SECONDS)
            }
        } else {
            // Reconnect
            steamClient.disconnect()
        }
    }

    private fun onLoggedOn(callback: LoggedOnCallback) {
        val result = callback.result

        when (result) {
            EResult.OK -> {
                // Successful login
                Log.i(TAG, "Logged on!")
                loginInProgress = false
                isLoggedIn = true

                currentRefreshToken?.let(::writeRefreshToken)
                val refreshToken = currentRefreshToken!!
                writeRefreshToken(refreshToken)

                val clientSteamId = steamClient.steamID!!
                steamId = clientSteamId.convertToUInt64()

                if (isPaused) {
                    showPausedNotification()
                } else if (waiting) {
                    updateNotification(getString(R.string.logged_in_elsewhere))
                } else {
                    updateNotification(getString(R.string.logged_in))
                }

                executor.execute {
                    val gotAuth = attemptWebAuthentication(clientSteamId, refreshToken)
                    if (gotAuth) {
                        resumeFarming()
                        registerApiKey()
                    } else {
                        updateNotification(getString(R.string.web_login_failed))
                    }
                }

                // TODO verify
                parentalStatus = callback.parentalSettings?.isEnabled ?: false
                val parentalIntent = Intent(PARENTAL_STATUS).apply {
                    putExtra("status", parentalStatus)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(parentalIntent)

                keyToRedeem?.let {
                    redeemKey(it)
                    keyToRedeem = null
                }
            }

            EResult.InvalidPassword if getRefreshToken().isNotEmpty() -> {
                // Refresh token no longer valid
                Log.i(TAG, "Refresh token expired")
                writeRefreshToken("")
                updateNotification(getString(R.string.login_key_expired))
                keyToRedeem = null
                steamClient.disconnect()
            }

            else -> {
                Log.i(TAG, "LogOn result: $result")
                keyToRedeem = null
                steamClient.disconnect()
            }
        }

        writeCellId(callback.cellID)

        sendLoginResult(result)
    }

    private fun onPurchaseResponse(callback: PurchaseResponseCallback) {
        if (callback.result == EResult.OK) {
            val kv: KeyValue = callback.purchaseReceiptInfo
            val paymentMethod = EPaymentMethod.from(kv["PaymentMethod"].asInteger())
            if (paymentMethod == EPaymentMethod.ActivationCode) {
                val products = StringBuilder()
                val size = kv["LineItemCount"].asInteger()
                Log.i(TAG, "LineItemCount $size")
                for (i in 0..<size) {
                    val lineItem = kv["lineitems"][i.toString()]["ItemDescription"].asString()
                    Log.i(TAG, "lineItem $i $lineItem")
                    products.append(lineItem)

                    if (i + 1 < size) products.append(", ")
                }
                showToast(getString(R.string.activated, products.toString()))
            }
        } else {
            val errorId = when (callback.purchaseResultDetail) {
                EPurchaseResultDetail.AlreadyPurchased -> R.string.product_already_owned
                EPurchaseResultDetail.BadActivationCode -> R.string.invalid_key
                else -> R.string.activation_failed
            }
            showToast(getString(errorId))
        }
    }

    private fun onPersonaState(callback: PersonaStateCallback) {
        if (callback.friendId == steamClient.steamID) {
            val personaName: String = callback.playerName
            val avatarHash = Strings.toHex(callback.avatarHash).lowercase(Locale.getDefault())
            Log.i(TAG, "Avatar hash $avatarHash")
            val event = Intent(PERSONA_EVENT).apply {
                putExtra(PERSONA_NAME, personaName)
                putExtra(AVATAR_HASH, avatarHash)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(event)
        }
    }

    private fun onFreeLicense(callback: FreeLicenseCallback) {
        val freeLicense = pendingFreeLicenses.removeFirst()
        if (callback.grantedApps.isNotEmpty()) {
            showToast(getString(R.string.activated, callback.grantedApps[0].toString()))
        } else if (callback.grantedPackages.isNotEmpty()) {
            showToast(getString(R.string.activated, callback.grantedPackages[0].toString()))
        } else {
            // Try activating it with the web handler
            executor.execute {
                val msg = if (webHandler.addFreeLicense(freeLicense)) {
                    getString(R.string.activated, freeLicense.toString())
                } else {
                    getString(R.string.activation_failed)
                }
                showToast(msg)
            }
        }
    }

    @Suppress("unused")
    private fun onAccountInfo(callback: AccountInfoCallback) {
        if (!getOffline()) {
            steamFriends.setPersonaState(EPersonaState.Online)
        }
    }

    private fun onItemAnnouncements(callback: ItemAnnouncementsCallback) {
        Log.i(TAG, "New item notification ${callback.count}")

        // Possible card drop
        if (callback.count > 0 && isFarming) executor.execute(farmTask)
    }

    /**
     * Idle one or more games
     * @param games the games to idle
     */
    private fun playGames(vararg games: Game) {
        val gamesPlayed = ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(
            CMsgClientGamesPlayed::class.java,
            EMsg.ClientGamesPlayed
        )
        for (game in games) {
            if (game.appId == 0) {
                // Non-Steam game
                val gameId = GameID(0).apply {
                    appType = GameID.GameType.SHORTCUT
                }
                val crc = CRC32().apply {
                    update(game.name.toByteArray())
                }
                // set the high-bit on the mod-id
                // reduces crc32 to 31bits, but lets us use the modID as a guaranteed unique
                // replacement for appID
                gameId.modID = crc.value or (-0x80000000).toLong()
                gamesPlayed.body.addGamesPlayedBuilder()
                    .setGameId(gameId.convertToUInt64())
                    .setGameExtraInfo(game.name)
            } else {
                gamesPlayed.body.addGamesPlayedBuilder().setGameId(game.appId.toLong())
            }
        }
        executor.execute { steamClient.send(gamesPlayed) }
        // Tell the activity
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(NOW_PLAYING_EVENT))
    }

    private fun stopPlaying() {
        if (!isPaused) {
            currentGames.clear()
        }
        val stopGame = ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(
            CMsgClientGamesPlayed::class.java,
            EMsg.ClientGamesPlayed
        )
        stopGame.body.addGamesPlayedBuilder().setGameId(0)
        executor.execute { steamClient.send(stopGame) }
        // Tell the activity
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(NOW_PLAYING_EVENT))
    }

    companion object {
        private val TAG: String = SteamService::class.java.simpleName
        private const val NOTIF_ID = 6896 // Ongoing notification ID
        private const val CHANNEL_ID = "idle_channel" // Notification channel

        // Some Huawei phones reportedly kill apps when they hold a WakeLock for a long time.
        // This can be prevented by using a WakeLock tag from the PowerGenie whitelist.
        private val WAKELOCK_TAG: String = "$TAG:LocationManagerService"

        private const val CUSTOM_OBFUSCATION_MASK = -0xff24553

        private const val FRIENDLY_NAME = "Idle Daddy-Fork, " + BuildConfig.VERSION_NAME

        // Events
        const val LOGIN_EVENT: String = "LOGIN_EVENT" // Emitted on login
        const val RESULT: String = "RESULT" // Login result
        const val DISCONNECT_EVENT: String = "DISCONNECT_EVENT" // Emitted on disconnect
        const val STOP_EVENT: String = "STOP_EVENT" // Emitted when stop clicked
        const val FARM_EVENT: String = "FARM_EVENT" // Emitted when farm() is called
        const val GAME_COUNT: String = "GAME_COUNT" // Number of games left to farm
        const val CARD_COUNT: String = "CARD_COUNT" // Number of card drops remaining

        // Emitted when we get PersonaStateCallback
        const val PERSONA_EVENT: String = "PERSONA_EVENT"

        // Emitted when we're logged in.
        const val PARENTAL_STATUS: String = "PARENTAL_STATUS"

        // Username
        const val PERSONA_NAME: String = "PERSONA_NAME"

        // User avatar hash
        const val AVATAR_HASH: String = "AVATAR_HASH"

        // Emitted when the game you're idling changes
        const val NOW_PLAYING_EVENT: String = "NOW_PLAYING_EVENT"

        // Actions
        const val SKIP_INTENT: String = "SKIP_INTENT"
        const val STOP_INTENT: String = "STOP_INTENT"
        const val PAUSE_INTENT: String = "PAUSE_INTENT"
        const val RESUME_INTENT: String = "RESUME_INTENT"

        fun createIntent(c: Context): Intent = Intent(c, SteamService::class.java)
    }
}