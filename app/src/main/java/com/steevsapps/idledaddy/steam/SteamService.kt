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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.MainActivity
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.handlers.PurchaseResponse
import com.steevsapps.idledaddy.handlers.callbacks.PurchaseResponseCallback
import com.steevsapps.idledaddy.listeners.AndroidLogListener
import com.steevsapps.idledaddy.preferences.PrefsManager.clearUser
import com.steevsapps.idledaddy.preferences.PrefsManager.hoursUntilDrops
import com.steevsapps.idledaddy.preferences.PrefsManager.loginKey
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.preferences.PrefsManager.offline
import com.steevsapps.idledaddy.preferences.PrefsManager.stayAwake
import com.steevsapps.idledaddy.preferences.PrefsManager.useCustomLoginId
import com.steevsapps.idledaddy.preferences.PrefsManager.username
import com.steevsapps.idledaddy.preferences.PrefsManager.writeLoginKey
import com.steevsapps.idledaddy.preferences.PrefsManager.writeSentryHash
import com.steevsapps.idledaddy.steam.SteamWebHandler.Companion.instance
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.utils.LocaleManager.setLocale
import com.steevsapps.idledaddy.utils.Utils.bytesToHex
import com.steevsapps.idledaddy.utils.Utils.calculateSHA1
import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPurchaseResultDetail
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2.CMsgClientRegisterKey
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.FreeLicenseCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.ItemAnnouncementsCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.MachineAuthDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.OTPDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.AccountInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoginKeyCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.UpdateMachineAuthCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.WebAPIUserNonceCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.ISteamConfigurationBuilder
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.types.GameID
import `in`.dragonbra.javasteam.util.NetHelpers
import `in`.dragonbra.javasteam.util.log.LogManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.NoSuchAlgorithmException
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32
import kotlin.concurrent.Volatile

class SteamService : Service() {

    private lateinit var steamClient: SteamClient
    private lateinit var steamApps: SteamApps
    private lateinit var steamFriends: SteamFriends
    private lateinit var steamUser: SteamUser
    private lateinit var manager: CallbackManager

    private val executor = Executors.newCachedThreadPool()
    private val pendingFreeLicenses = LinkedList<Int>()
    private val scheduler = Executors.newScheduledThreadPool(8)
    private val webHandler = instance
    private var farmHandle: ScheduledFuture<*>? = null
    private var farmIndex = 0
    private var gamesToFarm: List<Game>? = null
    private var isHuawei = false
    private var keyToRedeem: String? = null
    private var sentryFolder: File? = null
    private var waitHandle: ScheduledFuture<*>? = null
    private var wakeLock: WakeLock? = null

    var currentGames: MutableList<Game> = mutableListOf()
        private set

    var gameCount = 0
        private set

    var cardCount = 0
        private set

    var steamId: Long = 0
        private set

    var isLoggedIn = false
        private set

    private var logOnDetails: LogOnDetails? = null

    @Volatile
    private var running = false // Service running

    @Volatile
    private var connected = false // Connected to Steam

    @Volatile
    var isFarming = false // Currently farming
        private set

    @Volatile
    var isPaused = false // Game paused
        private set

    @Volatile
    private var waiting = false // Waiting for user to stop playing

    @Volatile
    private var loginInProgress = true // Currently logging in, so don't reconnect on disconnects

    // This is the object that receives interactions from clients.
    private val binder: IBinder = LocalBinder()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
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
    private val waitTask = Runnable {
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

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    inner class LocalBinder : Binder() {
        val service: SteamService
            get() = this@SteamService
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

        for (i in 0..2) {
            gamesToFarm = webHandler.remainingGames

            if (gamesToFarm == null) {
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

        if (gamesToFarm == null) {
            Log.i(TAG, "Invalid cookie data or no internet, reconnecting")
            // steamClient.disconnect();
            steamUser.requestWebAPIUserNonce()
            return
        }

        // Count the games and cards
        gameCount = gamesToFarm?.size ?: 0
        cardCount = 0

        gamesToFarm?.forEach { game ->
            cardCount += game.dropsRemaining
        }

        // Send farm event
        val event = Intent(FARM_EVENT)
        event.putExtra(GAME_COUNT, gameCount)
        event.putExtra(CARD_COUNT, cardCount)

        LocalBroadcastManager.getInstance(this@SteamService).sendBroadcast(event)

        if (gamesToFarm.isNullOrEmpty()) {
            Log.i(TAG, "Finished idling")
            stopPlaying()
            updateNotification(getString(R.string.idling_finished))
            stopFarming()
            return
        }

        // Sort by hours played descending
        Collections.sort(gamesToFarm!!, Collections.reverseOrder())
        if (farmIndex >= gamesToFarm!!.size) {
            farmIndex = 0
        }

        // TODO: Steam only updates play time every half hour, so maybe we should keep track of it ourselves
        val game = gamesToFarm!![farmIndex]
        if (game.hoursPlayed >= hoursUntilDrops || gamesToFarm!!.size == 1 || farmIndex > 0) {
            // Idle a single game
            Handler(Looper.getMainLooper()).post { idleSingle(game) }
            unscheduleFarmTask()
        } else {
            // Idle multiple games (max 32) until one has reached 2 hrs
            idleMultiple(gamesToFarm!!)
            scheduleFarmTask()
        }
    }

    fun skipGame() {
        if (gamesToFarm!!.size < 2) {
            return
        }

        farmIndex++

        if (farmIndex >= gamesToFarm!!.size) {
            farmIndex = 0
        }

        idleSingle(gamesToFarm!![farmIndex])
    }

    fun stopGame() {
        isPaused = false

        stopPlaying()
        stopFarming()

        updateNotification(getString(R.string.stopped))
        LocalBroadcastManager
            .getInstance(this@SteamService)
            .sendBroadcast(Intent(STOP_EVENT))
    }

    fun pauseGame() {
        isPaused = true

        stopPlaying()
        showPausedNotification()

        // Tell the activity to update
        LocalBroadcastManager
            .getInstance(this@SteamService)
            .sendBroadcast(Intent(NOW_PLAYING_EVENT))
    }

    fun resumeGame() {
        if (isFarming) {
            Log.i(TAG, "Resume farming")

            isPaused = false

            executor.execute(farmTask)
        } else if (currentGames.size == 1) {
            Log.i(TAG, "Resume playing")

            idleSingle(currentGames[0])
        } else if (currentGames.size > 1) {
            Log.i(TAG, "Resume playing (multiple)")

            idleMultiple(currentGames)
        }
    }

    private fun scheduleFarmTask() {
        if (farmHandle == null || farmHandle!!.isCancelled) {
            Log.i(TAG, "Starting farmtask")

            farmHandle = scheduler.scheduleAtFixedRate(farmTask, 10, 10, TimeUnit.MINUTES)
        }
    }

    private fun unscheduleFarmTask() {
        if (farmHandle != null) {
            Log.i(TAG, "Stopping farmtask")

            farmHandle!!.cancel(true)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Service created")

        sentryFolder = File(filesDir, "sentry")
        sentryFolder?.mkdirs()

        val config: SteamConfiguration =
            SteamConfiguration.create { b: ISteamConfigurationBuilder ->
                b.withServerListProvider(
                    FileServerListProvider(File(filesDir, "servers.bin"))
                )
            }

        steamClient = SteamClient(config)
        steamClient.addHandler(PurchaseResponse())

        steamUser = steamClient.getHandler(SteamUser::class.java)
        steamFriends = steamClient.getHandler(SteamFriends::class.java)
        steamApps = steamClient.getHandler(SteamApps::class.java)

        // Subscribe to callbacks
        manager = CallbackManager(steamClient)

        manager.subscribe(
            ConnectedCallback::class.java
        ) { callback: ConnectedCallback -> onConnected(callback) }

        manager.subscribe(
            DisconnectedCallback::class.java
        ) { callback: DisconnectedCallback -> onDisconnected(callback) }

        manager.subscribe(
            LoggedOffCallback::class.java
        ) { callback: LoggedOffCallback -> onLoggedOff(callback) }

        manager.subscribe(
            LoggedOnCallback::class.java
        ) { callback: LoggedOnCallback -> onLoggedOn(callback) }

        manager.subscribe(
            LoginKeyCallback::class.java
        ) { callback: LoginKeyCallback -> onLoginKey(callback) }

        manager.subscribe(
            UpdateMachineAuthCallback::class.java
        ) { callback: UpdateMachineAuthCallback -> onUpdateMachineAuth(callback) }

        manager.subscribe(
            PersonaStatesCallback::class.java
        ) { callback: PersonaStatesCallback -> onPersonaStates(callback) }

        manager.subscribe(
            FreeLicenseCallback::class.java
        ) { callback: FreeLicenseCallback -> onFreeLicense(callback) }

        manager.subscribe(
            AccountInfoCallback::class.java
        ) { callback: AccountInfoCallback -> onAccountInfo(callback) }

        manager.subscribe(
            WebAPIUserNonceCallback::class.java
        ) { callback: WebAPIUserNonceCallback -> onWebAPIUserNonce(callback) }

        manager.subscribe(
            ItemAnnouncementsCallback::class.java
        ) { callback: ItemAnnouncementsCallback -> onItemAnnouncements(callback) }

        manager.subscribe(
            PurchaseResponseCallback::class.java
        ) { callback: PurchaseResponseCallback -> onPurchaseResponse(callback) }

        // Detect Huawei devices running Lollipop which have a bug with MediaStyle notifications
        isHuawei = Build.MANUFACTURER.lowercase(Locale.getDefault()).contains("huawei")

        if (stayAwake()) {
            acquireWakeLock()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create notification channel
            createChannel()
        }

        if (BuildConfig.DEBUG) {
            LogManager.addListener(AndroidLogListener())
        }

        startForeground(NOTIFY_ID, buildNotification(getString(R.string.service_started)))
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(setLocale(base))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!running) {
            Log.i(TAG, "Command starting")
            val filter = IntentFilter()
            filter.addAction(SKIP_INTENT)
            filter.addAction(STOP_INTENT)
            filter.addAction(PAUSE_INTENT)
            filter.addAction(RESUME_INTENT)
            registerReceiver(receiver, filter)
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

        stopForeground(true)

        running = false

        stopFarming()

        executor.shutdownNow()
        scheduler.shutdownNow()

        releaseWakeLock()
        unregisterReceiver(receiver)

        super.onDestroy()
    }

    /**
     * Create notification channel for Android O
     */
    @RequiresApi(Build.VERSION_CODES.O)
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

    fun changeStatus(status: EPersonaState?) {
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

            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
            wakeLock?.acquire(60 * 60 * 1000L /*60 minutes*/)
        }
    }

    /**
     * Release the WakeLock
     */
    fun releaseWakeLock() {
        if (wakeLock != null) {
            Log.i(TAG, "Releasing WakeLock")
            wakeLock!!.release()
            wakeLock = null
        }
    }

    private fun buildNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            0
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
     *
     * @param game The [Game]
     */
    private fun showIdleNotification(game: Game) {
        Log.i(TAG, "Idle notification")

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            0
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                getString(
                    R.string.now_playing2,
                    if (game.appId == 0) {
                        getString(
                            R.string.playing_non_steam_game,
                            game.name
                        )
                    } else {
                        game.name
                    }
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)

        // MediaStyle causes a crash on certain Huawei devices running Lollipop
        // https://stackoverflow.com/questions/34851943/couldnt-expand-remoteviews-mediasessioncompat-and-notificationcompat-mediastyl
        if (!isHuawei) {
            builder.setStyle(androidx.media.app.NotificationCompat.MediaStyle())
        }

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
            Intent(STOP_INTENT),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val pauseIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(PAUSE_INTENT),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        builder.addAction(R.drawable.ic_action_stop, getString(R.string.stop), stopIntent)
        builder.addAction(R.drawable.ic_action_pause, getString(R.string.pause), pauseIntent)

        if (isFarming) {
            // Add the skip action
            val skipIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(SKIP_INTENT),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            builder.addAction(R.drawable.ic_action_skip, getString(R.string.skip), skipIntent)
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (!minimizeData()) {
            // Load game icon into notification
            Glide.with(applicationContext)
                .load(game.iconUrl)
                .asBitmap()
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        glideAnimation: GlideAnimation<in Bitmap>
                    ) {
                        builder.setLargeIcon(resource)
                        nm.notify(NOTIFY_ID, builder.build())
                    }

                    override fun onLoadFailed(e: Exception, errorDrawable: Drawable) {
                        super.onLoadFailed(e, errorDrawable)
                        nm.notify(NOTIFY_ID, builder.build())
                    }
                })
        } else {
            nm.notify(NOTIFY_ID, builder.build())
        }
    }

    /**
     * Show "Big Text" style notification with the games we're idling
     *
     * @param msg the games
     */
    private fun showMultipleNotification(msg: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            0
        )

        // Add stop and pause actions
        val stopIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(STOP_INTENT),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val pauseIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(PAUSE_INTENT),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.idling_multiple))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_action_stop, getString(R.string.stop), stopIntent)
            .addAction(R.drawable.ic_action_pause, getString(R.string.pause), pauseIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFY_ID, notification)
    }

    private fun showPausedNotification() {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)

        val resumeIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(RESUME_INTENT),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.paused))
            .setContentIntent(pi)
            .addAction(R.drawable.ic_action_play, getString(R.string.resume), resumeIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFY_ID, notification)
    }

    /**
     * Used to update the notification
     *
     * @param text the text to display
     */
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFY_ID, buildNotification(text))
    }

    private fun idleSingle(game: Game) {
        Log.i(TAG, "Now playing " + game.name)

        isPaused = false

        currentGames.clear()
        currentGames.add(game)

        playGames(game)

        showIdleNotification(game)
    }

    private fun idleMultiple(games: List<Game>) {
        Log.i(TAG, "Idling multiple")

        isPaused = false

        val gamesCopy: List<Game> = ArrayList(games)

        currentGames.clear()

        var size = gamesCopy.size
        if (size > 32) {
            size = 32
        }

        val msg = StringBuilder()
        for (i in 0 until size) {
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

        playGames(*currentGames.toTypedArray<Game>())

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

    fun addGames(games: List<Game>) {
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

        if (loginKey.isNotEmpty()) {
            // We can log in using saved credentials
            executor.execute { steamClient.connect() }
        }

        // Run the the callback handler
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

    fun login(details: LogOnDetails?) {
        Log.i(TAG, "logging in")

        loginInProgress = true
        logOnDetails = details

        executor.execute { steamClient.connect() }
    }

    fun logoff() {
        Log.i(TAG, "logging off")

        loginInProgress = true
        isLoggedIn = false
        steamId = 0
        logOnDetails = null
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
        if (!isLoggedIn && loginKey.isNotEmpty()) {
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
            } catch (e: NumberFormatException) {
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
        val registerKey: ClientMsgProtobuf<CMsgClientRegisterKey.Builder> =
            ClientMsgProtobuf<CMsgClientRegisterKey.Builder>(
                CMsgClientRegisterKey::class.java,
                EMsg.ClientRegisterKey
            )

        registerKey.body.setKey(productKey)

        executor.execute { steamClient.send(registerKey) }
    }

    /**
     * Perform log in. Needs to happen as soon as we connect or else we'll get an error
     */
    private fun doLogin() {
        if (useCustomLoginId()) {
            val localIp: Int = NetHelpers.getIPAddress(steamClient.localIP)
            logOnDetails?.loginID = localIp xor CUSTOM_OBFUSCATION_MASK
        }

        steamUser.logOn(logOnDetails)

        logOnDetails = null // No longer need this
    }

    /**
     * Log in using saved credentials
     */
    private fun attemptRestoreLogin() {
        val username = username
        val loginKey = loginKey

        if (username.isEmpty() || loginKey.isEmpty()) {
            return
        }

        Log.i(TAG, "Restoring login")

        val details = LogOnDetails()
        details.username = username
        details.loginKey = loginKey
        details.clientOSType = EOSType.LinuxUnknown

        if (useCustomLoginId()) {
            val localIp: Int = NetHelpers.getIPAddress(steamClient.localIP)
            details.loginID = localIp xor CUSTOM_OBFUSCATION_MASK
        }

        try {
            val sentryFile = File(sentryFolder, "$username.sentry")
            details.sentryFileHash = calculateSHA1(sentryFile)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        details.isShouldRememberPassword = true

        steamUser.logOn(details)
    }

    private fun attemptAuthentication(nonce: String): Boolean {
        Log.i(TAG, "Attempting SteamWeb authentication")

        for (i in 0..2) {
            if (webHandler.authenticate(steamClient, nonce)) {
                Log.i(TAG, "Authenticated!")
                return true
            }

            if (i + 1 < 3) {
                Log.i(TAG, "Retrying...")
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    return false
                }
            }
        }

        return false
    }

    private fun registerApiKey() {
        Log.i(TAG, "Registering API key")

        val result = webHandler.updateApiKey()

        Log.i(TAG, "API key result: $result")

        when (result) {
            SteamWebHandler.ApiKeyState.REGISTERED -> {}
            SteamWebHandler.ApiKeyState.ACCESS_DENIED -> showToast(getString(R.string.apikey_access_denied))
            SteamWebHandler.ApiKeyState.UNREGISTERED -> webHandler.updateApiKey() // Call updateApiKey once more to actually update it
            SteamWebHandler.ApiKeyState.ERROR -> showToast(getString(R.string.apikey_register_failed))
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onConnected(callback: ConnectedCallback) {
        Log.i(TAG, "Connected()")
        connected = true

        if (logOnDetails != null) {
            doLogin()
        } else {
            attemptRestoreLogin()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDisconnected(callback: DisconnectedCallback) {
        Log.i(TAG, "Disconnected()")

        connected = false
        isLoggedIn = false

        if (!loginInProgress) {
            // Try to reconnect after a 5 second delay
            scheduler.schedule(
                /* command = */ {
                    Log.i(TAG, "Reconnecting")
                    steamClient.connect()
                },
                /* delay = */ 5,
                /* unit = */ TimeUnit.SECONDS
            )
        } else {
            // SteamKit may disconnect us while logging on (if already connected),
            // but since it reconnects immediately after we do not have to reconnect here.
            Log.i(TAG, "NOT reconnecting (logon in progress)")
        }

        // Tell the activity that we've been disconnected from Steam
        LocalBroadcastManager.getInstance(this@SteamService).sendBroadcast(Intent(DISCONNECT_EVENT))
    }

    private fun onLoggedOff(callback: LoggedOffCallback) {
        Log.i(TAG, "Logoff result " + callback.result.toString())
        if (callback.result == EResult.LoggedInElsewhere) {
            updateNotification(getString(R.string.logged_in_elsewhere))
            unscheduleFarmTask()

            if (!waiting) {
                waiting = true
                waitHandle = scheduler.scheduleAtFixedRate(waitTask, 0, 30, TimeUnit.SECONDS)
            }
        } else {
            // Reconnect
            steamClient.disconnect()
        }
    }

    private fun onLoggedOn(callback: LoggedOnCallback) {
        val result: EResult = callback.result

        if (result == EResult.OK) {
            // Successful login
            Log.i(TAG, "Logged on!")

            loginInProgress = false
            isLoggedIn = true
            steamId = steamClient.steamID.convertToUInt64()

            if (isPaused) {
                showPausedNotification()
            } else if (waiting) {
                updateNotification(getString(R.string.logged_in_elsewhere))
            } else {
                updateNotification(getString(R.string.logged_in))
            }

            executor.execute {
                val gotAuth = attemptAuthentication(callback.webAPIUserNonce)
                if (gotAuth) {
                    resumeFarming()
                    registerApiKey()
                } else {
                    // Request a new WebAPI user authentication nonce
                    steamUser.requestWebAPIUserNonce()
                }
            }

            if (keyToRedeem != null) {
                redeemKey(keyToRedeem!!)
                keyToRedeem = null
            }
        } else if (result == EResult.InvalidPassword && loginKey.isNotEmpty()) {
            // Probably no longer valid
            Log.i(TAG, "Login key expired")

            writeLoginKey("")
            updateNotification(getString(R.string.login_key_expired))

            keyToRedeem = null
            steamClient.disconnect()
        } else {
            Log.i(TAG, "LogOn result: $result")

            keyToRedeem = null
            steamClient.disconnect()
        }

        // Tell LoginActivity the result
        val intent = Intent(LOGIN_EVENT)
        intent.putExtra(RESULT, result)

        LocalBroadcastManager.getInstance(this@SteamService).sendBroadcast(intent)
    }

    private fun onLoginKey(callback: LoginKeyCallback) {
        Log.i(TAG, "Saving loginkey")

        writeLoginKey(callback.loginKey)

        steamUser.acceptNewLoginKey(callback)
    }

    private fun onUpdateMachineAuth(callback: UpdateMachineAuthCallback) {
        val sentryFile = File(sentryFolder, "$username.sentry")

        Log.i(TAG, "Saving sentry file to " + sentryFile.absolutePath)

        try {
            FileOutputStream(sentryFile).use { fos ->
                val channel = fos.channel
                channel.position(callback.offset.toLong())
                channel.write(ByteBuffer.wrap(callback.data, 0, callback.bytesToWrite))

                val sha1 = calculateSHA1(sentryFile)

                val otp = OTPDetails()
                otp.identifier = callback.oneTimePassword.identifier
                otp.type = callback.oneTimePassword.type

                val auth = MachineAuthDetails()
                auth.jobID = callback.jobID
                auth.fileName = callback.fileName
                auth.bytesWritten = callback.bytesToWrite
                auth.fileSize = sentryFile.length().toInt()
                auth.offset = callback.offset
                auth.eResult = EResult.OK
                auth.lastError = 0
                auth.sentryFileHash = sha1
                auth.oneTimePassword = otp

                steamUser.sendMachineAuthResponse(auth)

                writeSentryHash(bytesToHex(sha1))
            }
        } catch (e: IOException) {
            Log.i(TAG, "Error saving sentry file", e)
        } catch (e: NoSuchAlgorithmException) {
            Log.i(TAG, "Error saving sentry file", e)
        }
    }

    private fun onPurchaseResponse(callback: PurchaseResponseCallback) {
        if (callback.result == EResult.OK) {
            val kv = callback.purchaseReceiptInfo
            val paymentMethod: EPaymentMethod = EPaymentMethod.from(kv["PaymentMethod"].asInteger())

            if (paymentMethod == EPaymentMethod.ActivationCode) {
                val products = StringBuilder()
                val size = kv["LineItemCount"].asInteger()

                Log.i(TAG, "LineItemCount $size")

                for (i in 0 until size) {
                    val lineItem = kv["lineitems"][i.toString() + ""]["ItemDescription"].asString()

                    Log.i(TAG, "lineItem $i $lineItem")

                    products.append(lineItem)

                    if (i + 1 < size) {
                        products.append(", ")
                    }
                }

                showToast(getString(R.string.activated, products.toString()))
            }
        } else {
            val errorId: Int = when (callback.purchaseResultDetails) {
                EPurchaseResultDetail.AlreadyPurchased -> R.string.product_already_owned
                EPurchaseResultDetail.BadActivationCode -> R.string.invalid_key
                else -> R.string.activation_failed
            }

            showToast(getString(errorId))
        }
    }

    private fun onPersonaStates(callback: PersonaStatesCallback) {
        for (ps in callback.personaStates) {
            if (ps.friendID == steamClient.steamID) {
                val personaName: String = ps.name
                val avatarHash = bytesToHex(ps.avatarHash).lowercase(Locale.getDefault())

                Log.i(TAG, "Avatar hash $avatarHash")

                val event = Intent(PERSONA_EVENT)
                event.putExtra(PERSONA_NAME, personaName)
                event.putExtra(AVATAR_HASH, avatarHash)

                LocalBroadcastManager.getInstance(this@SteamService).sendBroadcast(event)

                break
            }
        }
    }

    private fun onFreeLicense(callback: FreeLicenseCallback) {
        val freeLicense = pendingFreeLicenses.removeFirst()

        if (callback.grantedApps.isNotEmpty()) {
            showToast(getString(R.string.activated, callback.grantedApps[0].toString()))
        } else if (callback.grantedPackages.isNotEmpty()) {
            showToast(
                getString(
                    R.string.activated,
                    callback.grantedPackages[0].toString()
                )
            )
        } else {
            // Try activating it with the web handler
            executor.execute {
                val msg: String = if (webHandler.addFreeLicense(freeLicense)) {
                    getString(R.string.activated, freeLicense.toString())
                } else {
                    getString(R.string.activation_failed)
                }

                showToast(msg)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAccountInfo(callback: AccountInfoCallback) {
        if (!offline) {
            steamFriends.setPersonaState(EPersonaState.Online)
        }
    }

    private fun onWebAPIUserNonce(callback: WebAPIUserNonceCallback) {
        Log.i(TAG, "Got new WebAPI user authentication nonce")

        executor.execute {
            val gotAuth = attemptAuthentication(callback.nonce)
            if (gotAuth) {
                resumeFarming()
            } else {
                updateNotification(getString(R.string.web_login_failed))
            }
        }
    }

    private fun onItemAnnouncements(callback: ItemAnnouncementsCallback) {
        Log.i(TAG, "New item notification " + callback.count)

        if (callback.count > 0 && isFarming) {
            // Possible card drop
            executor.execute(farmTask)
        }
    }

    /**
     * Idle one or more games
     *
     * @param games the games to idle
     */
    private fun playGames(vararg games: Game) {
        val gamesPlayed: ClientMsgProtobuf<CMsgClientGamesPlayed.Builder> =
            ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(
                CMsgClientGamesPlayed::class.java,
                EMsg.ClientGamesPlayed
            )

        for (game in games) {
            if (game.appId == 0) {
                // Non-Steam game
                val gameId = GameID(game.appId)
                gameId.appType = GameID.GameType.SHORTCUT

                val crc = CRC32()
                crc.update(game.name.toByteArray())

                // set the high-bit on the mod-id
                // reduces crc32 to 31bits, but lets us use the modID as a guaranteed unique
                // replacement for appID
                gameId.setModID(crc.value or (-0x80000000).toLong())
                gamesPlayed.body.addGamesPlayedBuilder()
                    .setGameId(gameId.convertToUInt64())
                    .setGameExtraInfo(game.name)
            } else {
                gamesPlayed.body.addGamesPlayedBuilder()
                    .setGameId(game.appId.toLong())
            }
        }

        executor.execute { steamClient.send(gamesPlayed) }

        // Tell the activity
        LocalBroadcastManager
            .getInstance(this@SteamService)
            .sendBroadcast(Intent(NOW_PLAYING_EVENT))
    }

    private fun stopPlaying() {
        if (!isPaused) {
            currentGames.clear()
        }

        val stopGame: ClientMsgProtobuf<CMsgClientGamesPlayed.Builder> =
            ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(
                CMsgClientGamesPlayed::class.java,
                EMsg.ClientGamesPlayed
            )

        stopGame.body.addGamesPlayedBuilder().setGameId(0)

        executor.execute { steamClient.send(stopGame) }

        // Tell the activity
        LocalBroadcastManager
            .getInstance(this@SteamService)
            .sendBroadcast(Intent(NOW_PLAYING_EVENT))
    }

    /**
     * Register and idle a game for a few seconds to complete the Spring Cleaning daily tasks
     */
    fun registerAndIdle(game: String) {
        try {
            val appId = game.toInt()

            // Register the game
            steamApps.requestFreeLicense(appId)

            Thread.sleep(1000)

            // Play it for a few seconds
            val playGame: ClientMsgProtobuf<CMsgClientGamesPlayed.Builder> =
                ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(
                    CMsgClientGamesPlayed::class.java,
                    EMsg.ClientGamesPlayed
                )

            playGame.body.addGamesPlayedBuilder().setGameId(appId.toLong())

            steamClient.send(playGame)

            Thread.sleep(3000)

            // Stop playing
            playGame.body.clearGamesPlayed().addGamesPlayedBuilder().setGameId(0)

            steamClient.send(playGame)

            Thread.sleep(1000)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Open a cottage door (Winter Sale 2018)
     */
    @Suppress("unused")
    fun openCottageDoor() {
        executor.execute {
            val result = webHandler.openCottageDoor()
            if (result) {
                showToast(getString(R.string.door_success))
            } else {
                showToast(getString(R.string.door_fail))
            }
        }
    }

    companion object {
        private val TAG = SteamService::class.java.getSimpleName()

        private const val NOTIFY_ID = 6896 // Ongoing notification ID
        private const val CHANNEL_ID = "idle_channel" // Notification channel

        // Some Huawei phones reportedly kill apps when they hold a WakeLock for a long time.
        // This can be prevented by using a WakeLock tag from the PowerGenie whitelist.
        private val WAKELOCK_TAG = "$TAG:LocationManagerService"

        private const val CUSTOM_OBFUSCATION_MASK = -0xff24553

        // Events
        const val AVATAR_HASH = "AVATAR_HASH" // User avatar hash
        const val CARD_COUNT = "CARD_COUNT" // Number of card drops remaining
        const val DISCONNECT_EVENT = "DISCONNECT_EVENT" // Emitted on disconnect
        const val FARM_EVENT = "FARM_EVENT" // Emitted when farm() is called
        const val GAME_COUNT = "GAME_COUNT" // Number of games left to farm
        const val LOGIN_EVENT = "LOGIN_EVENT" // Emitted on login
        const val NOW_PLAYING_EVENT =
            "NOW_PLAYING_EVENT" // Emitted when the game you're idling changes
        const val PERSONA_EVENT = "PERSONA_EVENT" // Emitted when we get PersonaStateCallback
        const val PERSONA_NAME = "PERSONA_NAME" // Username
        const val RESULT = "RESULT" // Login result
        const val STOP_EVENT = "STOP_EVENT" // Emitted when stop clicked

        // Actions
        const val PAUSE_INTENT = "PAUSE_INTENT"
        const val RESUME_INTENT = "RESUME_INTENT"
        const val SKIP_INTENT = "SKIP_INTENT"
        const val STOP_INTENT = "STOP_INTENT"

        fun createIntent(c: Context?): Intent = Intent(c, SteamService::class.java)
    }
}
