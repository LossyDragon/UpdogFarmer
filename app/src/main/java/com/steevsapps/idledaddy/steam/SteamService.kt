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
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.MainActivity
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.listeners.AndroidLogListener
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.utils.LocaleManager
import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EPaymentMethod
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPurchaseResultDetail
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver.CMsgClientGamesPlayed
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2.CMsgClientRegisterKey
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesParentalSteamclient.CParental_ValidatePassword_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetOwnedGames_Request
import `in`.dragonbra.javasteam.rpc.service.Parental
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.CredentialsAuthSession
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
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
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.AccountInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.PlayingSessionStateCallback
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
import `in`.dragonbra.javasteam.util.IDebugNetworkListener
import `in`.dragonbra.javasteam.util.NetHelpers.getIPAddress
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.javasteam.util.log.LogManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.Closeable
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.zip.CRC32
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** UI-facing snapshot of the service, published to [SteamService.state]. */
@Immutable
data class SteamServiceState(
    val loggedIn: Boolean = false,
    val steamId: Long = 0,
    val farming: Boolean = false,
    val paused: Boolean = false,
    val parentalStatus: Boolean = false,
    val personaName: String = "",
    val avatarHash: String = "",
    val currentGames: List<Game> = emptyList(),
    val gameCount: Int = 0,
    val cardCount: Int = 0,
    val personaState: EPersonaState = EPersonaState.Offline,
    val blockedIdle: Boolean = false,
    val nextRetryAtMillis: Long = 0L,
    val itemAnnouncements: Int = 0,
)

class SteamService : Service() {
    // JavaSteam client and handlers
    private lateinit var steamClient: SteamClient
    private lateinit var manager: CallbackManager
    private lateinit var steamUser: SteamUser
    private lateinit var steamFriends: SteamFriends
    private lateinit var steamApps: SteamApps
    private lateinit var notifications: SteamNotifications
    private lateinit var steamUnifiedMessages: SteamUnifiedMessages
    private lateinit var player: Player
    private lateinit var parental: Parental

    private val webHandler: SteamWebHandler by inject()
    private val subscriptions: MutableList<Closeable> = mutableListOf()

    // Session state
    @Volatile
    private var running = false // Service running

    @Volatile
    private var connected = false // Connected to Steam

    private val isLoggedIn: Boolean
        get() = state.value.loggedIn

    // In-progress login flow
    @Volatile
    private var loginInProgress = true // Currently logging in, so don't reconnect on disconnects

    private var pendingAuthDetails: AuthSessionDetails? = null
    private var currentRefreshToken: String? = null

    @Volatile
    private var pendingQrLogin = false

    @Volatile
    private var pendingGuardCodeFuture: CompletableFuture<String>? = null

    // Farming/idling state
    private var farmIndex = 0
    private var gamesToFarm: MutableList<Game>? = null

    private val currentGames: List<Game>
        get() = state.value.currentGames
    private val isFarming: Boolean
        get() = state.value.farming
    private val isPaused: Boolean
        get() = state.value.paused

    // Currently blocked from idling because the account is playing on another device.
    // Reset optimistically on each logon; set by PlayingSessionStateCallback or a
    // LoggedInElsewhere kick.
    @Volatile
    private var blocked = false

    // Sticky "we were blocked recently" flag. Used to delay resuming after a block so
    // we don't hammer Steam with re-idle attempts and get repeatedly kicked. Modelled
    // on ArchiSteamFarm's PlayingWasBlocked + MinFarmingDelayAfterBlock.
    @Volatile
    private var playingWasBlocked = false

    // Number of consecutive failed resume attempts, used to grow the retry delay
    // exponentially. Reset once we resume successfully (or the user stops).
    @Volatile
    private var blockRetryCount = 0

    // Key redemption
    private var keyToRedeem: String? = null
    private val pendingFreeLicenses = ArrayDeque<Int>()

    // Background work
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var farmHandle: Job? = null
    private var resumeHandle: Job? = null
    private var backoffResetHandle: Job? = null
    private var wakeLock: WakeLock? = null

    /** Binder giving in-process clients direct access to the service. */
    inner class LocalBinder : Binder() {
        val service: SteamService
            get() = this@SteamService
    }

    /** Login flow events for the bound login UI, delivered on the main thread. */
    interface LoginEventListener {
        fun onLoginResult(result: EResult)
        fun onQrChallenge(url: String)
        fun onDeviceConfirmation()
    }

    var loginEventListener: LoginEventListener? = null

    val state: StateFlow<SteamServiceState>
        field = MutableStateFlow(SteamServiceState())

    // This is the object that receives interactions from clients.
    private val binder: IBinder = LocalBinder()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action ?: return) {
                SKIP_INTENT -> skipGame()
                STOP_INTENT -> stopGame()
                PAUSE_INTENT -> pauseGame()
                RESUME_INTENT -> resumeGame()
            }
        }
    }

    // Service lifecycle

    override fun onBind(intent: Intent): IBinder = binder

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onCreate() {
        Log.i(TAG, "Service created")
        super.onCreate()

        if (BuildConfig.DEBUG) {
            LogManager.addListener(AndroidLogListener())
        }

        val cellId = PrefsManager.getCellId()
        val servers = File(filesDir, "servers.bin")
        val fileServerListProvider = FileServerListProvider(servers)

        val config = SteamConfiguration.create {
            it.withServerListProvider(fileServerListProvider)
            if (cellId >= 0) it.withCellID(cellId)
        }
        steamClient = SteamClient(config)

        if (BuildConfig.DEBUG) {
            steamClient.debugNetworkListener = object : IDebugNetworkListener {
                override fun onIncomingNetworkMessage(msgType: EMsg, data: ByteArray) {
                    Log.d(TAG, "<< $msgType (${data.size}b)")
                }

                override fun onOutgoingNetworkMessage(msgType: EMsg, data: ByteArray) {
                    Log.d(TAG, ">> $msgType")
                }
            }
        }

        steamUser = requireNotNull(steamClient.getHandler())
        steamFriends = requireNotNull(steamClient.getHandler())
        steamApps = requireNotNull(steamClient.getHandler())
        notifications = requireNotNull(steamClient.getHandler())

        steamUnifiedMessages = requireNotNull(steamClient.getHandler())
        player = steamUnifiedMessages.createService()
        parental = steamUnifiedMessages.createService()

        // Catch ClientPlayingSessionState ourselves (see PlayingSessionMsgHandler / SteamKit #418).
        steamClient.addHandler(PlayingSessionMsgHandler())

        // Subscribe to callbacks
        manager = CallbackManager(steamClient)
        subscriptions.add(manager.subscribe(::onConnected))
        subscriptions.add(manager.subscribe(::onDisconnected))
        subscriptions.add(manager.subscribe(::onLoggedOff))
        subscriptions.add(manager.subscribe(::onLoggedOn))
        subscriptions.add(manager.subscribe(::onPlayingSessionState))
        subscriptions.add(manager.subscribe(::onPersonaState))
        subscriptions.add(manager.subscribe(::onFreeLicense))
        subscriptions.add(manager.subscribe(::onAccountInfo))
        subscriptions.add(manager.subscribe(::onItemAnnouncements))
        subscriptions.add(manager.subscribe(::onPurchaseResponse))

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

        if (PrefsManager.stayAwake()) {
            setWakeLock(true)
        }

        createChannel()

        startForeground(NOTIF_ID, notificationBuilder(getString(R.string.service_started)).build())
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
        scope.cancel()
        setWakeLock(false)
        unregisterReceiver(receiver)

        subscriptions.forEach { it.close() }

        super.onDestroy()
    }

    /** Connect with saved credentials (if any) and start the callback pump. */
    fun start() {
        running = true
        if (PrefsManager.getRefreshToken().isNotEmpty()) {
            // We can log in using saved credentials
            scope.launch { steamClient.connect() }
        }
        // Run the callback pump
        scope.launch {
            while (isActive) {
                try {
                    manager.runWaitCallbackAsync()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.i(TAG, "Callback pump failed", e)
                }
            }
        }
    }

    // Login

    /** Log in with username/password; 2FA codes are requested through [GuardCodeAuthenticator]. */
    fun login(username: String, password: String) {
        Log.i(TAG, "logging in")
        loginInProgress = true

        val details = AuthSessionDetails().apply {
            this.username = username
            this.password = password
            guardData = PrefsManager.getGuardData().takeIf { it.isNotEmpty() }
            persistentSession = true
            clientOSType = EOSType.AndroidUnknown
            deviceFriendlyName = FRIENDLY_NAME
            authenticator = GuardCodeAuthenticator()
        }

        pendingAuthDetails = details
        scope.launch { steamClient.connect() }
    }

    /**
     * Log in by QR code: emits [LoginEventListener.onQrChallenge], then waits for the user to
     * approve in the Steam Mobile App.
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
            scope.launch { steamClient.connect() }
        }
    }

    /** Supply the Steam Guard code requested during login. */
    fun submitTwoFactorCode(code: String) {
        pendingGuardCodeFuture?.complete(code)
    }

    /** Log out, clear the saved session, and reset login/farming state. */
    fun logoff() {
        Log.i(TAG, "logging off")
        loginInProgress = true
        pendingAuthDetails = null
        pendingQrLogin = false
        currentRefreshToken = null
        cancelPendingGuardCode()
        keyToRedeem = null
        pendingFreeLicenses.clear()
        stopFarming()
        state.update {
            it.copy(
                loggedIn = false,
                steamId = 0,
                personaName = "",
                avatarHash = "",
                currentGames = emptyList(),
                itemAnnouncements = 0,
            )
        }
        scope.launch {
            steamUser.logOff()
            steamClient.disconnect()
        }
        PrefsManager.clearUser()
        updateNotification(getString(R.string.logged_out))
    }

    /**
     * Run the credentials auth session on the service scope, then log on with the resulting
     * refresh token. Must start right after connecting.
     */
    private fun doCredentialsLogin(details: AuthSessionDetails) {
        scope.launch {
            try {
                val authSession: CredentialsAuthSession = steamClient.authentication
                    .beginAuthSessionViaCredentials(details)
                    .await()
                val pollResult: AuthPollResult = authSession.pollingWaitForResult().await()

                pollResult.newGuardData?.let { PrefsManager.writeGuardData(it) }
                performLogOn(pollResult.accountName, pollResult.refreshToken)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i(TAG, "Credentials login failed", e)
                cancelPendingGuardCode()
                keyToRedeem = null
                steamClient.disconnect()
                sendLoginResult(resolveFailureResult(e))
            }
        }
    }

    /** Run the QR auth session on the service scope, then log on once the user approves. */
    private fun doQrLogin() {
        scope.launch {
            try {
                val details = AuthSessionDetails().apply {
                    clientOSType = EOSType.AndroidUnknown
                    deviceFriendlyName = FRIENDLY_NAME
                    persistentSession = true
                }

                val authSession: QrAuthSession = steamClient.authentication
                    .beginAuthSessionViaQR(details)
                    .await()
                authSession.challengeUrlChanged = IChallengeUrlChanged { session ->
                    session?.let { sendQrChallengeUrl(it.challengeUrl) }
                }
                sendQrChallengeUrl(authSession.challengeUrl)

                val pollResult: AuthPollResult = authSession.pollingWaitForResult().await()

                performLogOn(pollResult.accountName, pollResult.refreshToken)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i(TAG, "QR login failed", e)
                keyToRedeem = null
                steamClient.disconnect()
                sendLoginResult(resolveFailureResult(e))
            }
        }
    }

    /** Log on to Steam3 with a refresh token (fresh login or restored session). */
    private fun performLogOn(username: String, refreshToken: String) {
        currentRefreshToken = refreshToken
        // Always keep this current: the QR login flow never types a username for LoginActivity to save
        PrefsManager.writeUsername(username)

        val details = LogOnDetails(
            username = username,
            accessToken = refreshToken,
            clientOSType = EOSType.AndroidUnknown,
            machineName = FRIENDLY_NAME,
            shouldRememberPassword = true,
            // chatMode = ChatMode.NEW_STEAM_CHAT,
            // uiMode = EUIMode.DesktopUI
        )
        if (PrefsManager.useCustomLoginId()) {
            val localIP = steamClient.localIP
            if (localIP != null) {
                details.loginID = getIPAddress(localIP) xor CUSTOM_OBFUSCATION_MASK
            }
        }
        steamUser.logOn(details)
    }

    /** Log on with the saved refresh token, if any. */
    private fun attemptRestoreLogin() {
        val username = PrefsManager.getUsername()
        val refreshToken = PrefsManager.getRefreshToken()
        if (username.isEmpty() || refreshToken.isEmpty()) return
        Log.i(TAG, "Restoring login")
        performLogOn(username, refreshToken)
    }

    /** Mint a web access token and authenticate on the Steam website. */
    private suspend fun attemptWebAuthentication(
        clientSteamId: SteamID,
        refreshToken: String,
    ): Boolean {
        Log.i(TAG, "Attempting SteamWeb authentication")
        val parentalToken = unlockParental()
        val authenticated = withRetries(retryDelay = 1.seconds) {
            try {
                val tokens = steamClient.authentication
                    .generateAccessTokenForApp(clientSteamId, refreshToken, true)
                    .await()
                webHandler.authenticate(
                    clientSteamId.convertToUInt64(),
                    tokens.accessToken,
                    parentalToken
                ).takeIf { it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i(TAG, "Failed to generate a web access token", e)
                null
            }
        } ?: false
        if (authenticated) {
            Log.i(TAG, "Authenticated!")
        }
        return authenticated
    }

    /** Unlock family view with the saved PIN, returning the unlock token. */
    private suspend fun unlockParental(): String? {
        val pin = PrefsManager.getParentalPin().trim()
        if (pin.isEmpty()) {
            return null
        }
        val request = CParental_ValidatePassword_Request.newBuilder().apply {
            password = pin
        }.build()
        return try {
            val response = parental.validatePassword(request).await()
            if (response.result != EResult.OK) {
                Log.i(TAG, "Parental unlock failed: ${response.result}")
                return null
            }
            response.body.token.ifEmpty { null }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Parental unlock failed", e)
            null
        }
    }

    /**
     * Bridges Steam Guard prompts to the login UI: parks a future in [pendingGuardCodeFuture],
     * signals the login screen, and waits for [submitTwoFactorCode].
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

    /** Park a future for the guard code and tell the login UI to ask for it. */
    private fun requestGuardCode(signal: EResult): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        pendingGuardCodeFuture = future
        sendLoginResult(signal)
        return future
    }

    /** Cancel the guard-code request of an abandoned login. */
    private fun cancelPendingGuardCode() {
        val future = pendingGuardCodeFuture
        pendingGuardCodeFuture = null
        future?.cancel(false)
    }

    /** Deliver a login event to the current listener on the main thread. */
    private fun notifyLoginListener(block: (LoginEventListener) -> Unit) {
        scope.launch(Dispatchers.Main) {
            loginEventListener?.let(block)
        }
    }

    /** Send a login result to the login UI. */
    private fun sendLoginResult(result: EResult) {
        notifyLoginListener { it.onLoginResult(result) }
    }

    /** Send the QR challenge [url] to the login UI. */
    private fun sendQrChallengeUrl(url: String) {
        notifyLoginListener { it.onQrChallenge(url) }
    }

    /** Map a login exception to the [EResult] shown to the UI. */
    private fun resolveFailureResult(e: Exception): EResult {
        val auth = e as? AuthenticationException ?: e.cause as? AuthenticationException
        return auth?.result ?: EResult.Fail
    }

    // Farming/idling controls

    /** Start farming card drops. */
    fun startFarming() {
        if (!isFarming) {
            state.update { it.copy(farming = true, paused = false) }
            launchFarm()
        }
    }

    /** Stop farming and clear the farm queue. */
    fun stopFarming() {
        if (isFarming) {
            gamesToFarm = null
            farmIndex = 0
            state.update { it.copy(farming = false, currentGames = emptyList()) }
            unscheduleFarmTask()
        }
    }

    /** Skip to the next game in the farm queue. */
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

    /** Stop idling entirely and clear pause/blocked state. */
    fun stopGame() {
        state.update { it.copy(paused = false) }
        playingWasBlocked = false
        blockRetryCount = 0
        backoffResetHandle?.cancel()
        unscheduleResume()
        setBlocked(false)
        stopPlaying()
        stopFarming()
        updateNotification(getString(R.string.stopped))
    }

    /** Pause idling, keeping [currentGames] so it can resume. */
    fun pauseGame() {
        state.update { it.copy(paused = true) }
        unscheduleResume()
        stopPlaying()
        showPausedNotification()
    }

    /** Resume idling or farming after a pause. */
    fun resumeGame() {
        if (resumeCurrentGames()) {
            return
        }
        if (isFarming) {
            Log.i(TAG, "Resume farming")
            state.update { it.copy(paused = false) }
            launchFarm()
        }
    }

    /** Add [game] to the currently idling set. */
    fun addGame(game: Game) {
        stopFarming()
        idleGames(currentGames + game)
    }

    /** Idle exactly [games], replacing the current set. */
    fun addGames(games: List<Game>) {
        stopFarming()
        idleGames(games)
    }

    /** Remove [game] from the currently idling set. */
    fun removeGame(game: Game) {
        stopFarming()
        idleGames(currentGames - game)
    }

    /** One farming pass: fetch remaining card drops and choose what to idle. */
    private suspend fun farm() {
        if (isPaused || blocked) {
            return
        }
        Log.i(TAG, "Checking remaining card drops")
        val games = withRetries(retryDelay = 500.milliseconds) { webHandler.remainingGames }
        if (games == null) {
            Log.i(TAG, "Invalid cookie data or no internet, reconnecting")
            steamClient.disconnect()
            return
        }
        gamesToFarm = games

        // Count the games and cards
        state.update { s ->
            s.copy(gameCount = games.size, cardCount = games.sumOf { it.dropsRemaining })
        }

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
        if (game.hoursPlayed >= PrefsManager.getHoursUntilDrops() || games.size == 1 || farmIndex > 0) {
            // Idle a single game
            withContext(Dispatchers.Main) { idleSingle(game) }
            unscheduleFarmTask()
        } else {
            // Idle multiple games (max 32) until one has reached 2 hrs
            idleMultiple(games)
            scheduleFarmTask()
        }
    }

    /** Run [farm], logging failures so periodic runs keep going. */
    private suspend fun farmSafely() {
        try {
            farm()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "FarmTask failed", e)
        }
    }

    /** Launch a one-off [farm] pass on the service scope. */
    private fun launchFarm() {
        scope.launch { farmSafely() }
    }

    /** Resume farming or idling unless paused or blocked. */
    private fun resumeFarming() {
        if (isPaused || blocked) {
            return
        }
        if (isFarming) {
            Log.i(TAG, "Resume farming")
            launchFarm()
        } else {
            resumeCurrentGames()
        }
    }

    /** Re-idle [currentGames]; returns false if there was nothing to resume. */
    private fun resumeCurrentGames(): Boolean {
        if (currentGames.size == 1) {
            Log.i(TAG, "Resume playing")
            scope.launch(Dispatchers.Main) { idleSingle(currentGames[0]) }
            return true
        }
        if (currentGames.size > 1) {
            Log.i(TAG, "Resume playing (multiple)")
            idleMultiple(currentGames)
            return true
        }
        return false
    }

    /** Idle the given games, or stop everything if the list is empty. */
    private fun idleGames(games: List<Game>) {
        when {
            games.size == 1 -> idleSingle(games[0])
            games.size > 1 -> idleMultiple(games)
            else -> stopGame()
        }
    }

    /** Idle a single game and show its notification. */
    private fun idleSingle(game: Game) {
        Log.i(TAG, "Now playing ${game.name}")
        state.update { it.copy(paused = false, currentGames = listOf(game)) }
        playGames(listOf(game))
        showIdleNotification(game)
    }

    /** Idle up to 32 games at once and show the big-text notification. */
    private fun idleMultiple(games: List<Game>) {
        Log.i(TAG, "Idling multiple")
        val playing = games.take(32)
        state.update { it.copy(paused = false, currentGames = playing) }
        playGames(playing)
        showMultipleNotification(playing.joinToString("\n") { gameDisplayName(it) })
    }

    /** Schedule the periodic drop check. */
    private fun scheduleFarmTask() {
        if (farmHandle?.isActive != true) {
            Log.i(TAG, "Starting farmtask")
            farmHandle = scope.launch {
                while (isActive) {
                    delay(10.minutes)
                    farmSafely()
                }
            }
        }
    }

    /** Cancel the periodic drop check. */
    private fun unscheduleFarmTask() {
        farmHandle?.let {
            Log.i(TAG, "Stopping farmtask")
            it.cancel()
        }
    }

    /** Tell Steam we're playing [games]. */
    private fun playGames(games: List<Game>) {
        sendGamesPlayed {
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
                    addGamesPlayedBuilder()
                        .setGameId(gameId.convertToUInt64())
                        .setGameExtraInfo(game.name)
                } else {
                    addGamesPlayedBuilder().setGameId(game.appId.toLong())
                }
            }
        }
    }

    /** Tell Steam we stopped playing. */
    private fun stopPlaying() {
        if (!isPaused) {
            state.update { it.copy(currentGames = emptyList()) }
        }
        sendGamesPlayed { addGamesPlayedBuilder().setGameId(0) }
    }

    /** Build a ClientGamesPlayed message with [configure] and send it. */
    private fun sendGamesPlayed(configure: CMsgClientGamesPlayed.Builder.() -> Unit) {
        val gamesPlayed = ClientMsgProtobuf<CMsgClientGamesPlayed.Builder>(
            CMsgClientGamesPlayed::class.java,
            EMsg.ClientGamesPlayed
        )
        gamesPlayed.body.configure()
        scope.launch { steamClient.send(gamesPlayed) }
    }

    // Blocked-idle handling (account playing on another device)

    /** Update the blocked state and reflect it in the notification and UI. */
    private fun setBlocked(value: Boolean, appId: Int = 0) {
        blocked = value
        publishOccupied()
        if (value) {
            unscheduleFarmTask()
            unscheduleResume()
            // A block cancels a pending "resume succeeded" reset so the backoff keeps growing.
            backoffResetHandle?.cancel()
            val text = if (appId > 0) {
                getString(R.string.steam_in_use_app, appId)
            } else {
                getString(R.string.logged_in_elsewhere)
            }
            updateNotification(text)
        }
    }

    /** Publish to the UI whether idling is blocked or waiting out the post-block delay. */
    private fun publishOccupied() {
        val occupied = blocked || playingWasBlocked
        state.update { it.copy(blockedIdle = occupied) }
    }

    /** Schedule a resume attempt after a block, backing off exponentially per failed attempt. */
    private fun scheduleResumeAfterBlock() {
        unscheduleResume()
        val exp = blockRetryCount.coerceAtMost(16)
        val delaySecs = (RESUME_BACKOFF_BASE_SECS shl exp).coerceAtMost(RESUME_BACKOFF_MAX_SECS)
        blockRetryCount++

        val nextAt = System.currentTimeMillis() + delaySecs * 1000L
        state.update { it.copy(nextRetryAtMillis = nextAt) }
        Log.i(TAG, "Will attempt to resume idling in ${delaySecs}s (attempt $blockRetryCount)")

        resumeHandle = scope.launch {
            delay(delaySecs.seconds)
            playingWasBlocked = false
            state.update { it.copy(nextRetryAtMillis = 0L) }
            publishOccupied()
            resumeFarming()
            // If we stay unblocked for a grace period, treat the resume as successful and
            // reset the backoff.
            scheduleBackoffReset()
        }
    }

    /** Reset the backoff once we stay unblocked for a grace period. */
    private fun scheduleBackoffReset() {
        backoffResetHandle?.cancel()
        backoffResetHandle = scope.launch {
            delay(BACKOFF_RESET_GRACE_SECS.seconds)
            Log.i(TAG, "Resume looks stable, resetting backoff")
            blockRetryCount = 0
        }
    }

    /** Cancel any pending resume attempt. */
    private fun unscheduleResume() {
        resumeHandle?.cancel()
        resumeHandle = null
        state.update { it.copy(nextRetryAtMillis = 0L) }
    }

    /**
     * The account started or stopped playing on another device: stop idling while blocked,
     * resume (after a delay) once the block clears.
     */
    private fun handlePlayingSessionState(isBlocked: Boolean, appId: Int) {
        Log.i(TAG, "PlayingSessionState blocked=$isBlocked appId=$appId")
        if (isBlocked == blocked) {
            return
        }
        if (isBlocked) {
            // The user started playing on another device. Stop idling (keep currentGames
            // so we can resume the same games later). setBlocked handles notification/UI.
            playingWasBlocked = true
            setBlocked(true, appId)
        } else {
            // Steam explicitly told us the other device stopped, so this isn't a failed
            // retry: reset the backoff and resume from the base delay.
            blockRetryCount = 0
            setBlocked(false)
            if (!isPaused) {
                updateNotification(getString(R.string.logged_in))
                scheduleResumeAfterBlock()
            }
        }
    }

    /**
     * Catches ClientPlayingSessionState by numeric code (9600) to work around the
     * ClientConcurrentSessionsBase EMsg collision (SteamKit #418).
     */
    private inner class PlayingSessionMsgHandler : ClientMsgHandler() {
        override fun handleMsg(packetMsg: IPacketMsg) {
            if (packetMsg.msgType.code() != EMsg.ClientPlayingSessionState.code()) {
                return
            }
            val cb = PlayingSessionStateCallback(packetMsg)
            handlePlayingSessionState(cb.isPlayingBlocked, cb.playingAppID)
        }
    }

    // Key redemption

    /** Redeem a product key, or activate a free license if [key] is numeric. */
    fun redeemKey(key: String) {
        if (!isLoggedIn && PrefsManager.getRefreshToken().isNotEmpty()) {
            Log.i(TAG, "Will redeem key at login")
            keyToRedeem = key
            return
        }
        Log.i(TAG, "Redeeming key...")
        if (key.isNotEmpty() && key.all(Char::isDigit)) {
            // Request a free license
            val freeLicense = key.toIntOrNull()
            if (freeLicense != null) {
                addFreeLicense(freeLicense)
            } else {
                showToast(getString(R.string.invalid_key))
            }
        } else {
            // Register product key
            registerProductKey(key)
        }
    }

    /** Request a free license by app id. */
    private fun addFreeLicense(freeLicense: Int) {
        pendingFreeLicenses.add(freeLicense)
        scope.launch { steamApps.requestFreeLicense(freeLicense) }
    }

    /** Send a product key to Steam for activation. */
    private fun registerProductKey(productKey: String) {
        val registerKey = ClientMsgProtobuf<CMsgClientRegisterKey.Builder>(
            CMsgClientRegisterKey::class.java,
            EMsg.ClientRegisterKey
        )
        registerKey.body.key = productKey
        scope.launch { steamClient.send(registerKey) }
    }

    // Misc public API

    /** Apply the online/offline preference to our persona state. */
    fun changeStatus() {
        val personaState =
            if (PrefsManager.getOffline()) EPersonaState.Offline else EPersonaState.Online
        if (isLoggedIn) {
            scope.launch { steamFriends.setPersonaState(personaState) }
            state.update { it.copy(personaState = personaState) }
        }
    }

    /** Acquire or release the CPU wake lock. */
    fun setWakeLock(acquire: Boolean = PrefsManager.stayAwake()) {
        if (acquire) {
            if (wakeLock == null) {
                Log.i(TAG, "Acquiring WakeLock")
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
                lock.acquire(60 * 60 * 1000L) // 60 Minutes
                wakeLock = lock
            }
        } else {
            val lock = wakeLock
            if (lock != null) {
                Log.i(TAG, "Releasing WakeLock")
                lock.release()
                wakeLock = null
            }
        }
    }

    /** Clear the new-item announcement count. */
    fun clearItemAnnouncements() {
        state.update { it.copy(itemAnnouncements = 0) }
    }

    /** Fetch the account's owned games as a total count and list. */
    suspend fun getOwnedGames(): Pair<Int, List<Game>> {
        val request = CPlayer_GetOwnedGames_Request.newBuilder().apply {
            steamid = state.value.steamId
            includePlayedFreeGames = PrefsManager.includeFreeGames()
            includeAppinfo = true
        }.build()
        val result = player.getOwnedGames(request).await().body
        return result.gameCount to result.gamesList.map {
            Game(
                appId = it.appid,
                name = it.name,
                iconUrl = "https://shared.fastly.steamstatic.com/store_item_assets/" +
                    "steam/apps/${it.appid}/header.jpg",
                hoursPlayed = it.playtimeForever / 60f,
            )
        }
    }

    // Steam callbacks

    /** Connected: start the pending login flow or restore the saved session. */
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

    /** Disconnected: reconnect after a delay unless a login is in progress. */
    @Suppress("unused")
    private fun onDisconnected(callback: DisconnectedCallback) {
        Log.i(TAG, "Disconnected()")
        connected = false
        state.update { it.copy(loggedIn = false) }

        if (!loginInProgress) {
            // Try to reconnect after a 5-second delay
            scope.launch {
                delay(5.seconds)
                Log.i(TAG, "Reconnecting")
                steamClient.connect()
            }
        } else {
            // SteamKit may disconnect us while logging on (if already connected),
            // but since it reconnects immediately after we do not have to reconnect here.
            Log.i(TAG, "NOT reconnecting (logon in progress)")
        }
    }

    /** Logged off: handle blocked/revoked sessions, otherwise trigger a reconnect. */
    private fun onLoggedOff(callback: LoggedOffCallback) {
        Log.i(TAG, "Logoff result ${callback.result}")
        if (callback.result == EResult.LoggedInElsewhere) {
            // Steam kicked us because the account started playing on another device.
            // (Steam does NOT proactively push PlayingSessionState in this ordering, it
            // just kicks us when we send ClientGamesPlayed.) Remember we were blocked so
            // that after reconnecting we delay before retrying instead of tight-looping.
            playingWasBlocked = true
            setBlocked(true)
        } else if (callback.result == EResult.Revoked) {
            // Our authentication has been revoked by the user.
            // Clear the session instead of reconnecting.
            showToast(getString(R.string.login_revoked))
            logoff()
            return
        }

        // Reconnect
        steamClient.disconnect()
    }

    /** Logon result: on success persist the session, authenticate on the web, and resume. */
    private fun onLoggedOn(callback: LoggedOnCallback) {
        val result = callback.result

        when (result) {
            EResult.OK -> {
                // Successful login
                Log.i(TAG, "Logged on!")
                loginInProgress = false

                val refreshToken = requireNotNull(currentRefreshToken)
                PrefsManager.writeRefreshToken(refreshToken)

                val clientSteamId = steamClient.steamID!!
                state.update {
                    it.copy(
                        loggedIn = true,
                        steamId = clientSteamId.convertToUInt64(),
                        // TODO verify
                        parentalStatus = callback.parentalSettings?.isEnabled ?: false,
                    )
                }

                // Optimistically assume we're free to play again. If we're still blocked
                // Steam will tell us (PlayingSessionState) or kick us (LoggedInElsewhere).
                setBlocked(false)

                if (isPaused) {
                    showPausedNotification()
                } else if (playingWasBlocked) {
                    updateNotification(getString(R.string.logged_in_elsewhere))
                } else {
                    updateNotification(getString(R.string.logged_in))
                }

                scope.launch {
                    val gotAuth = attemptWebAuthentication(clientSteamId, refreshToken)
                    if (gotAuth) {
                        // If we were just blocked, wait out the delay before retrying so
                        // we don't immediately get kicked again; otherwise resume now.
                        if (playingWasBlocked) {
                            scheduleResumeAfterBlock()
                        } else {
                            resumeFarming()
                        }
                    } else {
                        updateNotification(getString(R.string.web_login_failed))
                    }
                }

                keyToRedeem?.let {
                    redeemKey(it)
                    keyToRedeem = null
                }

                notifications.requestItemAnnouncements()
            }

            EResult.InvalidPassword if PrefsManager.getRefreshToken().isNotEmpty() -> {
                // Refresh token no longer valid
                Log.i(TAG, "Refresh token expired")
                PrefsManager.writeRefreshToken("")
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

        PrefsManager.writeCellId(callback.cellID)

        sendLoginResult(result)
    }

    /**
     * Never fires today: EMsg 9600 collides with ClientConcurrentSessionsBase (SteamKit #418),
     * so [PlayingSessionMsgHandler] catches it instead. Kept in case JavaSteam fixes the routing.
     */
    private fun onPlayingSessionState(callback: PlayingSessionStateCallback) {
        handlePlayingSessionState(callback.isPlayingBlocked, callback.playingAppID)
    }

    /** Update our persona name/state/avatar. */
    private fun onPersonaState(callback: PersonaStateCallback) {
        if (callback.friendId == steamClient.steamID) {
            state.update {
                it.copy(
                    personaName = callback.playerName,
                    personaState = callback.personaState,
                    avatarHash = Strings.toHex(callback.avatarHash)
                )
            }
        }
    }

    /** Toast the result of a free-license request. */
    private fun onFreeLicense(callback: FreeLicenseCallback) {
        val freeLicense = pendingFreeLicenses.removeFirst()
        if (callback.grantedApps.isNotEmpty()) {
            showToast(getString(R.string.activated, callback.grantedApps[0].toString()))
        } else if (callback.grantedPackages.isNotEmpty()) {
            showToast(getString(R.string.activated, callback.grantedPackages[0].toString()))
        } else {
            // Try activating it with the web handler
            scope.launch {
                val msg = if (webHandler.addFreeLicense(freeLicense)) {
                    getString(R.string.activated, freeLicense.toString())
                } else {
                    getString(R.string.activation_failed)
                }
                showToast(msg)
            }
        }
    }

    /** Toast the result of a product-key activation. */
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

    /** Go online once account info arrives, unless the offline pref is set. */
    @Suppress("unused")
    private fun onAccountInfo(callback: AccountInfoCallback) {
        if (!PrefsManager.getOffline()) {
            steamFriends.setPersonaState(EPersonaState.Online)
        }
    }

    /** New inventory items: update the badge and re-check drops while farming. */
    private fun onItemAnnouncements(callback: ItemAnnouncementsCallback) {
        Log.i(TAG, "New item notification ${callback.count}")
        state.update { it.copy(itemAnnouncements = callback.count) }

        // Possible card drop
        if (callback.count > 0 && isFarming) launchFarm()
    }

    // Notifications

    /** Create the notification channel. */
    private fun createChannel() {
        val name: CharSequence = getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.enableVibration(false)
        channel.enableLights(false)
        channel.setBypassDnd(false)
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    /** PendingIntent that opens [MainActivity]. */
    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    /** PendingIntent broadcasting a notification action ([SKIP_INTENT] etc.). */
    private fun actionIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        this,
        0,
        Intent(action).setPackage(packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
    )

    /** Base notification: app icon and title, [text], opens the app on tap. */
    private fun notificationBuilder(text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(contentIntent())

    /** Replace the ongoing notification's text. */
    private fun updateNotification(text: String) {
        notificationManager.notify(NOTIF_ID, notificationBuilder(text).build())
    }

    /** Media-style notification for a single idled game with stop/pause(/skip) actions. */
    private fun showIdleNotification(game: Game) {
        Log.i(TAG, "Idle notification")

        @Suppress("DEPRECATION")
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()

        val builder = notificationBuilder(getString(R.string.now_playing2, gameDisplayName(game)))
            .setPriority(NotificationCompat.PRIORITY_MAX)
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

        builder.addAction(
            R.drawable.ic_action_stop,
            getString(R.string.stop),
            actionIntent(STOP_INTENT)
        )
        builder.addAction(
            R.drawable.ic_action_pause,
            getString(R.string.pause),
            actionIntent(PAUSE_INTENT)
        )

        if (isFarming) {
            builder.addAction(
                R.drawable.ic_action_skip,
                getString(R.string.skip),
                actionIntent(SKIP_INTENT)
            )
        }

        if (!PrefsManager.minimizeData()) {
            // Load game icon into notification
            val request = ImageRequest.Builder(applicationContext)
                .data(game.iconUrl)
                // Notification#setLargeIcon requires a software bitmap, not a hardware one
                .allowHardware(false)
                .target(
                    onSuccess = { result ->
                        builder.setLargeIcon(result.toBitmap())
                        notificationManager.notify(NOTIF_ID, builder.build())
                    },
                    onError = {
                        notificationManager.notify(NOTIF_ID, builder.build())
                    },
                )
                .build()
            applicationContext.imageLoader.enqueue(request)
        } else {
            notificationManager.notify(NOTIF_ID, builder.build())
        }
    }

    /** Big-text notification listing the games being idled. */
    private fun showMultipleNotification(msg: String) {
        val notification = notificationBuilder(getString(R.string.idling_multiple))
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(
                R.drawable.ic_action_stop,
                getString(R.string.stop),
                actionIntent(STOP_INTENT)
            )
            .addAction(
                R.drawable.ic_action_pause,
                getString(R.string.pause),
                actionIntent(PAUSE_INTENT)
            )
            .build()
        notificationManager.notify(NOTIF_ID, notification)
    }

    /** Notification with a resume action while paused. */
    private fun showPausedNotification() {
        val notification = notificationBuilder(getString(R.string.paused))
            .addAction(
                R.drawable.ic_action_play,
                getString(R.string.resume),
                actionIntent(RESUME_INTENT)
            )
            .build()
        notificationManager.notify(NOTIF_ID, notification)
    }

    /** Game name, marked as non-Steam when appId is 0. */
    private fun gameDisplayName(game: Game): String =
        if (game.appId == 0) getString(R.string.playing_non_steam_game, game.name) else game.name

    // Helpers

    /** Show a toast from any thread. */
    private fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    /** Run [block] up to [attempts] times, delaying [retryDelay] between tries, until non-null. */
    private suspend fun <T : Any> withRetries(
        retryDelay: Duration,
        attempts: Int = 3,
        block: suspend () -> T?,
    ): T? {
        repeat(attempts) { attempt ->
            block()?.let { return it }
            if (attempt + 1 < attempts) {
                Log.i(TAG, "Retrying...")
                delay(retryDelay)
            }
        }
        return null
    }

    companion object {
        private val TAG: String = SteamService::class.java.simpleName
        private const val NOTIF_ID = 6896 // Ongoing notification ID
        private const val CHANNEL_ID = "idle_channel" // Notification channel

        // Exponential backoff for retrying to idle after being blocked by the account
        // playing elsewhere. Avoids a tight kick/reconnect loop. Base matches ASF's 60s
        // default; the delay doubles each consecutive failed attempt up to the cap.
        private const val RESUME_BACKOFF_BASE_SECS = 60L
        private const val RESUME_BACKOFF_MAX_SECS = 1800L
        private const val BACKOFF_RESET_GRACE_SECS = 120L

        // Some Huawei phones reportedly kill apps when they hold a WakeLock for a long time.
        // This can be prevented by using a WakeLock tag from the PowerGenie whitelist.
        private val WAKELOCK_TAG: String = "$TAG:LocationManagerService"

        private const val CUSTOM_OBFUSCATION_MASK = -0xff24553

        private const val FRIENDLY_NAME = "Idle Daddy-Fork, " + BuildConfig.VERSION_NAME

        // Actions
        const val SKIP_INTENT: String = "SKIP_INTENT"
        const val STOP_INTENT: String = "STOP_INTENT"
        const val PAUSE_INTENT: String = "PAUSE_INTENT"
        const val RESUME_INTENT: String = "RESUME_INTENT"

        fun createIntent(c: Context): Intent = Intent(c, SteamService::class.java)
    }
}