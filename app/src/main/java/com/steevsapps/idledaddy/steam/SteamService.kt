package com.steevsapps.idledaddy.steam;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.app.NotificationCompat.MediaStyle;

import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.steevsapps.idledaddy.BuildConfig;
import com.steevsapps.idledaddy.MainActivity;
import com.steevsapps.idledaddy.R;
import com.steevsapps.idledaddy.handlers.PurchaseResponse;
import com.steevsapps.idledaddy.handlers.callbacks.PurchaseResponseCallback;
import com.steevsapps.idledaddy.listeners.AndroidLogListener;
import com.steevsapps.idledaddy.preferences.PrefsManager;
import com.steevsapps.idledaddy.steam.model.Game;
import com.steevsapps.idledaddy.utils.LocaleManager;
import com.steevsapps.idledaddy.utils.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import in.dragonbra.javasteam.base.ClientMsgProtobuf;
import in.dragonbra.javasteam.enums.EMsg;
import in.dragonbra.javasteam.enums.EOSType;
import in.dragonbra.javasteam.enums.EPaymentMethod;
import in.dragonbra.javasteam.enums.EPersonaState;
import in.dragonbra.javasteam.enums.EPurchaseResultDetail;
import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver;
import in.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2;
import in.dragonbra.javasteam.steam.authentication.AuthPollResult;
import in.dragonbra.javasteam.steam.authentication.AuthSessionDetails;
import in.dragonbra.javasteam.steam.authentication.AuthenticationException;
import in.dragonbra.javasteam.steam.authentication.CredentialsAuthSession;
import in.dragonbra.javasteam.steam.authentication.IAuthenticator;
import in.dragonbra.javasteam.steam.authentication.QrAuthSession;
import in.dragonbra.javasteam.steam.discovery.FileServerListProvider;
import in.dragonbra.javasteam.steam.handlers.steamapps.SteamApps;
import in.dragonbra.javasteam.steam.handlers.steamapps.callback.FreeLicenseCallback;
import in.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends;
import in.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback;
import in.dragonbra.javasteam.steam.handlers.steamnotifications.callback.ItemAnnouncementsCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails;
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.AccountInfoCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback;
import in.dragonbra.javasteam.steam.steamclient.SteamClient;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;
import in.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration;
import in.dragonbra.javasteam.types.GameID;
import in.dragonbra.javasteam.types.KeyValue;
import in.dragonbra.javasteam.types.SteamID;
import in.dragonbra.javasteam.util.NetHelpers;
import in.dragonbra.javasteam.util.log.LogManager;

public class SteamService extends Service {
    private final static String TAG = SteamService.class.getSimpleName();
    private final static int NOTIF_ID = 6896; // Ongoing notification ID
    private final static String CHANNEL_ID = "idle_channel"; // Notification channel
    // Some Huawei phones reportedly kill apps when they hold a WakeLock for a long time.
    // This can be prevented by using a WakeLock tag from the PowerGenie whitelist.
    private final static String WAKELOCK_TAG = TAG + ":LocationManagerService";
    private final static int CUSTOM_OBFUSCATION_MASK = 0xF00DBAAD;
    private final static String FRIENDLY_NAME = "Idle Daddy-Fork, " + BuildConfig.VERSION_NAME;

    // Events
    public final static String LOGIN_EVENT = "LOGIN_EVENT"; // Emitted on login
    public final static String RESULT = "RESULT"; // Login result
    public final static String DISCONNECT_EVENT = "DISCONNECT_EVENT"; // Emitted on disconnect
    public final static String STOP_EVENT = "STOP_EVENT"; // Emitted when stop clicked
    public final static String FARM_EVENT = "FARM_EVENT"; // Emitted when farm() is called
    public final static String GAME_COUNT = "GAME_COUNT"; // Number of games left to farm
    public final static String CARD_COUNT = "CARD_COUNT"; // Number of card drops remaining
    public final static String PERSONA_EVENT = "PERSONA_EVENT"; // Emitted when we get PersonaStateCallback
    public final static String PERSONA_NAME = "PERSONA_NAME"; // Username
    public final static String AVATAR_HASH = "AVATAR_HASH"; // User avatar hash
    public final static String NOW_PLAYING_EVENT = "NOW_PLAYING_EVENT"; // Emitted when the game you're idling changes
    public final static String QR_CHALLENGE_EVENT = "QR_CHALLENGE_EVENT"; // Emitted when the QR login URL is available/refreshed
    public final static String QR_URL = "QR_URL"; // The QR login challenge URL
    public final static String DEVICE_CONFIRMATION_EVENT = "DEVICE_CONFIRMATION_EVENT"; // Emitted when awaiting Steam Mobile App approval

    // Actions
    public final static String SKIP_INTENT = "SKIP_INTENT";
    public final static String STOP_INTENT = "STOP_INTENT";
    public final static String PAUSE_INTENT = "PAUSE_INTENT";
    public final static String RESUME_INTENT = "RESUME_INTENT";

    private SteamClient steamClient;
    private CallbackManager manager;
    private SteamUser steamUser;
    private SteamFriends steamFriends;
    private SteamApps steamApps;
    private final SteamWebHandler webHandler = SteamWebHandler.getInstance();
    private PowerManager.WakeLock wakeLock;
    private final List<Closeable> subscriptions = new ArrayList<>();

    private int farmIndex = 0;
    private List<Game> gamesToFarm;
    private final List<Game> currentGames = new ArrayList<>();
    private int gameCount = 0;
    private int cardCount = 0;

    private AuthSessionDetails pendingAuthDetails = null;
    private volatile boolean pendingQrLogin = false;
    private String currentRefreshToken = null;
    private volatile CompletableFuture<String> pendingGuardCodeFuture = null;

    private volatile boolean running = false; // Service running
    private volatile boolean connected = false; // Connected to Steam
    private volatile boolean farming = false; // Currently farming
    private volatile boolean paused = false; // Game paused
    private volatile boolean waiting = false; // Waiting for user to stop playing
    private volatile boolean loginInProgress = true; // Currently logging in, so don't reconnect on disconnects

    private long steamId;
    private boolean loggedIn = false;
    private boolean isHuawei = false;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private ScheduledFuture<?> farmHandle;
    private ScheduledFuture<?> waitHandle;

    private String keyToRedeem = null;
    private final LinkedList<Integer> pendingFreeLicenses = new LinkedList<>();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public SteamService getService() {
            return SteamService.this;
        }
    }

    // This is the object that receives interactions from clients.
    private final IBinder binder = new LocalBinder();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            var action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case SKIP_INTENT:
                    skipGame();
                    break;
                case STOP_INTENT:
                    stopGame();
                    break;
                case PAUSE_INTENT:
                    pauseGame();
                    break;
                case RESUME_INTENT:
                    resumeGame();
                    break;
            }
        }
    };

    private final Runnable farmTask = () -> {
        try {
            farm();
        } catch (Exception e) {
            Log.i(TAG, "FarmTask failed", e);
        }
    };

    /**
     * Wait for user to NOT be in-game so we can resume idling
     */
    private final Runnable waitTask = new Runnable() {
        @Override
        public void run() {
            try {
                Log.i(TAG, "Checking if we can resume idling...");
                final Boolean notInGame = webHandler.checkIfNotInGame();
                if (notInGame == null) {
                    Log.i(TAG, "Invalid cookie data or no internet, reconnecting...");
                    steamClient.disconnect();
                } else if (notInGame) {
                    Log.i(TAG, "Resuming...");
                    waiting = false;
                    steamClient.disconnect();
                    waitHandle.cancel(false);
                }
            } catch (Exception e) {
                Log.i(TAG, "WaitTask failed", e);
            }
        }
    };

    public void startFarming() {
        if (!farming) {
            farming = true;
            paused = false;
            executor.execute(farmTask);
        }
    }

    public void stopFarming() {
        if (farming) {
            farming = false;
            gamesToFarm = null;
            farmIndex = 0;
            currentGames.clear();
            unscheduleFarmTask();
        }
    }

    /**
     * Resume farming/idling
     */
    private void resumeFarming() {
        if (paused || waiting) {
            return;
        }

        if (farming) {
            Log.i(TAG, "Resume farming");
            executor.execute(farmTask);
        } else if (currentGames.size() == 1) {
            Log.i(TAG, "Resume playing");
            new Handler(Looper.getMainLooper()).post(() -> idleSingle(currentGames.get(0)));
        } else if (currentGames.size() > 1) {
            Log.i(TAG, "Resume playing (multiple)");
            idleMultiple(currentGames);
        }
    }

    private void farm() {
        if (paused || waiting) {
            return;
        }
        Log.i(TAG, "Checking remaining card drops");
        for (int i = 0; i < 3; i++) {
            gamesToFarm = webHandler.getRemainingGames();
            if (gamesToFarm != null) {
                Log.i(TAG, "gotem");
                break;
            }
            if (i + 1 < 3) {
                Log.i(TAG, "retrying...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        if (gamesToFarm == null) {
            Log.i(TAG, "Invalid cookie data or no internet, reconnecting");
            steamClient.disconnect();
            return;
        }

        // Count the games and cards
        gameCount = gamesToFarm.size();
        cardCount = 0;
        for (Game g : gamesToFarm) {
            cardCount += g.dropsRemaining;
        }

        // Send farm event
        final Intent event = new Intent(FARM_EVENT);
        event.putExtra(GAME_COUNT, gameCount);
        event.putExtra(CARD_COUNT, cardCount);
        LocalBroadcastManager.getInstance(SteamService.this)
                .sendBroadcast(event);

        if (gamesToFarm.isEmpty()) {
            Log.i(TAG, "Finished idling");
            stopPlaying();
            updateNotification(getString(R.string.idling_finished));
            stopFarming();
            return;
        }

        // Sort by hours played descending
        Collections.sort(gamesToFarm, Collections.reverseOrder());

        if (farmIndex >= gamesToFarm.size()) {
            farmIndex = 0;
        }
        final Game game = gamesToFarm.get(farmIndex);

        // TODO: Steam only updates play time every half hour, so maybe we should keep track of it ourselves
        if (game.hoursPlayed >= PrefsManager.getHoursUntilDrops() || gamesToFarm.size() == 1 || farmIndex > 0) {
            // Idle a single game
            new Handler(Looper.getMainLooper()).post(() -> idleSingle(game));
            unscheduleFarmTask();
        } else {
            // Idle multiple games (max 32) until one has reached 2 hrs
            idleMultiple(gamesToFarm);
            scheduleFarmTask();
        }
    }

    public void skipGame() {
        if (gamesToFarm == null || gamesToFarm.size() < 2) {
            return;
        }

        farmIndex++;
        if (farmIndex >= gamesToFarm.size()) {
            farmIndex = 0;
        }

        idleSingle(gamesToFarm.get(farmIndex));
    }

    public void stopGame() {
        paused = false;
        stopPlaying();
        stopFarming();
        updateNotification(getString(R.string.stopped));
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(new Intent(STOP_EVENT));
    }

    public void pauseGame() {
        paused = true;
        stopPlaying();
        showPausedNotification();
        // Tell the activity to update
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(new Intent(NOW_PLAYING_EVENT));
    }

    public void resumeGame() {
        if (farming) {
            Log.i(TAG, "Resume farming");
            paused = false;
            executor.execute(farmTask);
        } else if (currentGames.size() == 1) {
            Log.i(TAG, "Resume playing");
            idleSingle(currentGames.get(0));
        } else if (currentGames.size() > 1) {
            Log.i(TAG, "Resume playing (multiple)");
            idleMultiple(currentGames);
        }
    }

    private void scheduleFarmTask() {
        if (farmHandle == null || farmHandle.isCancelled()) {
            Log.i(TAG, "Starting farmtask");
            farmHandle = scheduler.scheduleWithFixedDelay(farmTask, 10, 10, TimeUnit.MINUTES);
        }
    }

    private void unscheduleFarmTask() {
        if (farmHandle != null) {
            Log.i(TAG, "Stopping farmtask");
            farmHandle.cancel(true);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public static Intent createIntent(Context c) {
        return new Intent(c, SteamService.class);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service created");
        super.onCreate();

        var cellId = PrefsManager.getCellId();
        var servers = new File(getFilesDir(), "servers.bin");
        var fileServerListProvider = new FileServerListProvider(servers);

        final SteamConfiguration config = SteamConfiguration.create(b -> {
            b.withServerListProvider(fileServerListProvider);
            if (cellId >= 0) {
                b.withCellID(cellId);
            }
        });

        steamClient = new SteamClient(config);
        steamClient.addHandler(new PurchaseResponse());
        steamUser = steamClient.getHandler(SteamUser.class);
        steamFriends = steamClient.getHandler(SteamFriends.class);
        steamApps = steamClient.getHandler(SteamApps.class);

        // Subscribe to callbacks
        manager = new CallbackManager(steamClient);
        subscriptions.add(manager.subscribe(ConnectedCallback.class, this::onConnected));
        subscriptions.add(manager.subscribe(DisconnectedCallback.class, this::onDisconnected));
        subscriptions.add(manager.subscribe(LoggedOffCallback.class, this::onLoggedOff));
        subscriptions.add(manager.subscribe(LoggedOnCallback.class, this::onLoggedOn));
        subscriptions.add(manager.subscribe(PersonaStateCallback.class, this::onPersonaState));
        subscriptions.add(manager.subscribe(FreeLicenseCallback.class, this::onFreeLicense));
        subscriptions.add(manager.subscribe(AccountInfoCallback.class, this::onAccountInfo));
        subscriptions.add(manager.subscribe(ItemAnnouncementsCallback.class, this::onItemAnnouncements));
        subscriptions.add(manager.subscribe(PurchaseResponseCallback.class, this::onPurchaseResponse));

        // Detect Huawei devices running Lollipop which have a bug with MediaStyle notifications
        isHuawei = (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1 ||
                android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) &&
                Build.MANUFACTURER.toLowerCase(Locale.getDefault()).contains("huawei");
        if (PrefsManager.stayAwake()) {
            acquireWakeLock();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create notification channel
            createChannel();
        }
        if (BuildConfig.DEBUG) {
            LogManager.addListener(new AndroidLogListener());
        }
        startForeground(NOTIF_ID, buildNotification(getString(R.string.service_started)));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.setLocale(base));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            Log.i(TAG, "Command starting");
            final IntentFilter filter = new IntentFilter();
            filter.addAction(SKIP_INTENT);
            filter.addAction(STOP_INTENT);
            filter.addAction(PAUSE_INTENT);
            filter.addAction(RESUME_INTENT);
            ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            start();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed");
        new Thread(() -> {
            steamUser.logOff();
            steamClient.disconnect();
        }).start();
        stopForeground(true);
        running = false;
        stopFarming();
        executor.shutdownNow();
        scheduler.shutdownNow();
        releaseWakeLock();
        unregisterReceiver(receiver);

        for (var subscription : subscriptions) {
            try {
                subscription.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        super.onDestroy();
    }

    /**
     * Create notification channel for Android O
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        final CharSequence name = getString(R.string.channel_name);
        final int importance = NotificationManager.IMPORTANCE_LOW;
        final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.enableVibration(false);
        channel.enableLights(false);
        channel.setBypassDnd(false);
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isFarming() {
        return farming;
    }

    public boolean isPaused() {
        return paused;
    }

    /**
     * Get the games we're currently idling
     */
    public ArrayList<Game> getCurrentGames() {
        return new ArrayList<>(currentGames);
    }

    public int getGameCount() {
        return gameCount;
    }

    public int getCardCount() {
        return cardCount;
    }

    public long getSteamId() {
        return steamId;
    }

    public void changeStatus(EPersonaState status) {
        if (isLoggedIn()) {
            executor.execute(() -> steamFriends.setPersonaState(status));
        }
    }

    /**
     * Acquire WakeLock to keep the CPU from sleeping
     */
    public void acquireWakeLock() {
        if (wakeLock == null) {
            Log.i(TAG, "Acquiring WakeLock");
            final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
            wakeLock.acquire(60 * 60 * 1000L); // 60 Minutes
        }
    }

    /**
     * Release the WakeLock
     */
    public void releaseWakeLock() {
        if (wakeLock != null) {
            Log.i(TAG, "Releasing WakeLock");
            wakeLock.release();
            wakeLock = null;
        }
    }

    private Notification buildNotification(String text) {
        final Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .build();
    }

    /**
     * Show idling notification
     *
     * @param game the {@link Game} object
     */
    private void showIdleNotification(Game game) {
        Log.i(TAG, "Idle notification");
        final Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.now_playing2,
                        (game.appId == 0) ? getString(R.string.playing_non_steam_game, game.name) : game.name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent);

        // MediaStyle causes a crash on certain Huawei devices running Lollipop
        // https://stackoverflow.com/questions/34851943/couldnt-expand-remoteviews-mediasessioncompat-and-notificationcompat-mediastyl
        if (!isHuawei) {
            builder.setStyle(new MediaStyle());
        }

        if (game.dropsRemaining > 0) {
            // Show drops remaining
            builder.setSubText(getResources().getQuantityString(R.plurals.card_drops_remaining, game.dropsRemaining, game.dropsRemaining));
        }

        // Add the stop and pause actions
        final PendingIntent stopIntent = PendingIntent.getBroadcast(this, 0, new Intent(STOP_INTENT).setPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        final PendingIntent pauseIntent = PendingIntent.getBroadcast(this, 0, new Intent(PAUSE_INTENT).setPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        builder.addAction(R.drawable.ic_action_stop, getString(R.string.stop), stopIntent);
        builder.addAction(R.drawable.ic_action_pause, getString(R.string.pause), pauseIntent);

        if (farming) {
            // Add the skip action
            final PendingIntent skipIntent = PendingIntent.getBroadcast(this, 0, new Intent(SKIP_INTENT).setPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(R.drawable.ic_action_skip, getString(R.string.skip), skipIntent);
        }

        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!PrefsManager.minimizeData()) {
            // Load game icon into notification
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(game.iconUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            builder.setLargeIcon(resource);
                            nm.notify(NOTIF_ID, builder.build());
                        }

                        @Override
                        public void onLoadFailed(Drawable errorDrawable) {
                            nm.notify(NOTIF_ID, builder.build());
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                        }
                    });
        } else {
            nm.notify(NOTIF_ID, builder.build());
        }
    }

    /**
     * Show "Big Text" style notification with the games we're idling
     *
     * @param msg the games
     */
    private void showMultipleNotification(String msg) {
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        // Add stop and pause actions
        final PendingIntent stopIntent = PendingIntent.getBroadcast(this, 0, new Intent(STOP_INTENT).setPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        final PendingIntent pauseIntent = PendingIntent.getBroadcast(this, 0, new Intent(PAUSE_INTENT).setPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

        final Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.idling_multiple))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_action_stop, getString(R.string.stop), stopIntent)
                .addAction(R.drawable.ic_action_pause, getString(R.string.pause), pauseIntent)
                .build();

        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, notification);
    }

    private void showPausedNotification() {
        final PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        final PendingIntent resumeIntent = PendingIntent.getBroadcast(this, 0, new Intent(RESUME_INTENT).setPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        final Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.paused))
                .setContentIntent(pi)
                .addAction(R.drawable.ic_action_play, getString(R.string.resume), resumeIntent)
                .build();
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, notification);
    }

    /**
     * Used to update the notification
     *
     * @param text the text to display
     */
    private void updateNotification(String text) {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIF_ID, buildNotification(text));
    }

    private void idleSingle(Game game) {
        Log.i(TAG, "Now playing " + game.name);
        paused = false;
        currentGames.clear();
        currentGames.add(game);
        playGames(game);
        showIdleNotification(game);
    }

    private void idleMultiple(List<Game> games) {
        Log.i(TAG, "Idling multiple");
        paused = false;
        final List<Game> gamesCopy = new ArrayList<>(games);
        currentGames.clear();

        int size = gamesCopy.size();
        if (size > 32) {
            size = 32;
        }

        final StringBuilder msg = new StringBuilder();
        for (int i = 0; i < size; i++) {
            final Game game = gamesCopy.get(i);
            currentGames.add(game);
            if (game.appId == 0) {
                // Non-Steam game
                msg.append(getString(R.string.playing_non_steam_game, game.name));
            } else {
                msg.append(game.name);
            }
            if (i + 1 < size) {
                msg.append("\n");
            }
        }

        playGames(currentGames.toArray(new Game[0]));
        showMultipleNotification(msg.toString());
    }

    public void addGame(Game game) {
        stopFarming();
        if (currentGames.isEmpty()) {
            idleSingle(game);
        } else {
            currentGames.add(game);
            idleMultiple(currentGames);
        }
    }

    public void addGames(List<Game> games) {
        stopFarming();
        if (games.size() == 1) {
            idleSingle(games.get(0));
        } else if (games.size() > 1) {
            idleMultiple(games);
        } else {
            stopGame();
        }
    }

    public void removeGame(Game game) {
        stopFarming();
        currentGames.remove(game);
        if (currentGames.size() == 1) {
            idleSingle(currentGames.get(0));
        } else if (currentGames.size() > 1) {
            idleMultiple(currentGames);
        } else {
            stopGame();
        }
    }

    public void start() {
        running = true;
        if (!PrefsManager.getRefreshToken().isEmpty()) {
            // We can log in using saved credentials
            executor.execute(() -> steamClient.connect());
        }
        // Run the the callback handler
        executor.execute(() -> {
            while (running) {
                try {
                    manager.runWaitCallbacks(1000L);
                } catch (Exception e) {
                    Log.i(TAG, "update() failed", e);
                }
            }
        });
    }

    /**
     * Log in with a fresh username/password. Two-factor codes (if needed) are requested afterward
     * through {@link GuardCodeAuthenticator} and supplied via {@link #submitTwoFactorCode(String)}.
     */
    public void login(String username, String password) {
        Log.i(TAG, "logging in");
        loginInProgress = true;

        final AuthSessionDetails details = new AuthSessionDetails();
        details.username = username;
        details.password = password;
        details.persistentSession = true;
        details.clientOSType = EOSType.AndroidUnknown;
        details.deviceFriendlyName = FRIENDLY_NAME;
        details.authenticator = new GuardCodeAuthenticator();

        pendingAuthDetails = details;
        executor.execute(() -> steamClient.connect());
    }

    /**
     * Log in by scanning a QR code with the Steam Mobile App. No password/2FA code is needed;
     * the QR auth session broadcasts QR_CHALLENGE_EVENT with the URL to render as soon as it begins,
     * then blocks until the user approves the prompt on their phone.
     */
    public void loginWithQr() {
        Log.i(TAG, "logging in via QR");
        loginInProgress = true;
        if (connected) {
            // Already connected (e.g. left over from a prior login attempt) - connect() won't fire
            // another ConnectedCallback, so start the QR session directly or we'd never show a code.
            doQrLogin();
        } else {
            pendingQrLogin = true;
            executor.execute(() -> steamClient.connect());
        }
    }

    /**
     * Supply the Steam Guard code requested by {@link GuardCodeAuthenticator} for the in-progress login.
     */
    public void submitTwoFactorCode(String code) {
        final CompletableFuture<String> future = pendingGuardCodeFuture;
        if (future != null) {
            future.complete(code);
        }
    }

    public void logoff() {
        Log.i(TAG, "logging off");
        loginInProgress = true;
        loggedIn = false;
        steamId = 0;
        pendingAuthDetails = null;
        pendingQrLogin = false;
        currentRefreshToken = null;
        cancelPendingGuardCode();
        currentGames.clear();
        keyToRedeem = null;
        pendingFreeLicenses.clear();
        stopFarming();
        executor.execute(() -> {
            steamUser.logOff();
            steamClient.disconnect();
        });
        PrefsManager.clearUser();
        updateNotification(getString(R.string.logged_out));
    }

    /**
     * Redeem Steam key or activate free license
     */
    public void redeemKey(String key) {
        if (!loggedIn && !PrefsManager.getRefreshToken().isEmpty()) {
            Log.i(TAG, "Will redeem key at login");
            keyToRedeem = key;
            return;
        }
        Log.i(TAG, "Redeeming key...");
        if (key.matches("\\d+")) {
            // Request a free license
            try {
                int freeLicense = Integer.parseInt(key);
                addFreeLicense(freeLicense);
            } catch (NumberFormatException e) {
                showToast(getString(R.string.invalid_key));
            }
        } else {
            // Register product key
            registerProductKey(key);
        }
    }

    /**
     * Request a free license
     */
    private void addFreeLicense(int freeLicense) {
        pendingFreeLicenses.add(freeLicense);
        executor.execute(() -> steamApps.requestFreeLicense(freeLicense));
    }

    /**
     * Register a product key
     */
    private void registerProductKey(String productKey) {
        final ClientMsgProtobuf<SteammessagesClientserver2.CMsgClientRegisterKey.Builder> registerKey;
        registerKey = new ClientMsgProtobuf<>(SteammessagesClientserver2.CMsgClientRegisterKey.class, EMsg.ClientRegisterKey);
        registerKey.getBody().setKey(productKey);
        executor.execute(() -> steamClient.send(registerKey));
    }

    /**
     * Begin a fresh credentials-based auth session (username/password + 2FA), then log on with the
     * resulting refresh token once it completes. Runs on a background thread since both
     * {@code beginAuthSessionViaCredentials} and {@code pollingWaitForResult} block until Steam
     * responds (possibly waiting on {@link #submitTwoFactorCode(String)}).
     * Needs to happen as soon as we connect or else we'll get an error.
     */
    private void doCredentialsLogin(AuthSessionDetails details) {
        executor.execute(() -> {
            try {
                final CredentialsAuthSession authSession = steamClient.getAuthentication()
                        .beginAuthSessionViaCredentials(details)
                        .get();
                final AuthPollResult pollResult = authSession.pollingWaitForResult().get();

                PrefsManager.writeGuardData(pollResult.getNewGuardData() != null ? pollResult.getNewGuardData() : "");
                performLogOn(pollResult.getAccountName(), pollResult.getRefreshToken());
            } catch (Exception e) {
                Log.i(TAG, "Credentials login failed", e);
                cancelPendingGuardCode();
                keyToRedeem = null;
                steamClient.disconnect();
                sendLoginResult(resolveFailureResult(e));
            }
        });
    }

    /**
     * Begin a fresh QR auth session and log on with the resulting refresh token once the user
     * approves the prompt in the Steam Mobile App. Runs on a background thread since both
     * {@code beginAuthSessionViaQR} and {@code pollingWaitForResult} block until Steam responds.
     */
    private void doQrLogin() {
        executor.execute(() -> {
            try {
                final AuthSessionDetails details = new AuthSessionDetails();
                details.clientOSType = EOSType.AndroidUnknown;
                details.deviceFriendlyName = FRIENDLY_NAME;
                details.persistentSession = true;

                final QrAuthSession authSession = steamClient.getAuthentication()
                        .beginAuthSessionViaQR(details)
                        .get();
                authSession.setChallengeUrlChanged(session -> sendQrChallengeUrl(session.getChallengeUrl()));
                sendQrChallengeUrl(authSession.getChallengeUrl());

                final AuthPollResult pollResult = authSession.pollingWaitForResult().get();

                PrefsManager.writeGuardData(pollResult.getNewGuardData() != null ? pollResult.getNewGuardData() : "");
                performLogOn(pollResult.getAccountName(), pollResult.getRefreshToken());
            } catch (Exception e) {
                Log.i(TAG, "QR login failed", e);
                keyToRedeem = null;
                steamClient.disconnect();
                sendLoginResult(resolveFailureResult(e));
            }
        });
    }

    private void sendQrChallengeUrl(String url) {
        final Intent intent = new Intent(QR_CHALLENGE_EVENT);
        intent.putExtra(QR_URL, url);
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(intent);
    }

    /**
     * Log on to the Steam3 network using a refresh token, used both for a freshly completed
     * credentials login and for restoring a saved session.
     */
    private void performLogOn(String username, String refreshToken) {
        currentRefreshToken = refreshToken;
        // Always keep this current: the QR login flow never types a username for LoginActivity to save
        PrefsManager.writeUsername(username);

        final LogOnDetails details = new LogOnDetails();
        details.setUsername(username);
        details.setAccessToken(refreshToken);
        details.setClientOSType(EOSType.AndroidUnknown);
        details.setMachineName(FRIENDLY_NAME);
        details.setShouldRememberPassword(true);
        if (PrefsManager.useCustomLoginId()) {
            final int localIp = NetHelpers.getIPAddress(steamClient.getLocalIP());
            details.setLoginID(localIp ^ CUSTOM_OBFUSCATION_MASK);
        }
        steamUser.logOn(details);
    }

    /**
     * Log in using a saved refresh token
     */
    private void attemptRestoreLogin() {
        final String username = PrefsManager.getUsername();
        final String refreshToken = PrefsManager.getRefreshToken();
        if (username.isEmpty() || refreshToken.isEmpty()) {
            return;
        }
        Log.i(TAG, "Restoring login");
        performLogOn(username, refreshToken);
    }

    /**
     * Mint a fresh web access token from our refresh token and use it to authenticate on the Steam website.
     */
    private boolean attemptWebAuthentication(SteamID clientSteamId, String refreshToken) {
        Log.i(TAG, "Attempting SteamWeb authentication");
        for (int i = 0; i < 3; i++) {
            try {
                final var tokens = steamClient.getAuthentication()
                        .generateAccessTokenForApp(clientSteamId, refreshToken, true)
                        .get();
                if (webHandler.authenticate(clientSteamId.convertToUInt64(), tokens.getAccessToken())) {
                    Log.i(TAG, "Authenticated!");
                    return true;
                }
            } catch (Exception e) {
                Log.i(TAG, "Failed to generate a web access token", e);
            }

            if (i + 1 < 3) {
                Log.i(TAG, "Retrying...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Authenticator bridging JavaSteam's poll-based Steam Guard prompts to LoginActivity's UI: each
     * callback parks a future in {@link #pendingGuardCodeFuture} and tells LoginActivity (via
     * LOGIN_EVENT, reusing the EResults it already branches on) to show the code field. The actual
     * code arrives later through {@link #submitTwoFactorCode(String)}.
     */
    private final class GuardCodeAuthenticator implements IAuthenticator {
        @Override
        public CompletableFuture<String> getDeviceCode(boolean previousCodeWasIncorrect) {
            return requestGuardCode(previousCodeWasIncorrect
                    ? EResult.TwoFactorCodeMismatch
                    : EResult.AccountLoginDeniedNeedTwoFactor);
        }

        @Override
        public CompletableFuture<String> getEmailCode(String email, boolean previousCodeWasIncorrect) {
            return requestGuardCode(previousCodeWasIncorrect
                    ? EResult.InvalidLoginAuthCode
                    : EResult.AccountLogonDenied);
        }

        @Override
        public CompletableFuture<Boolean> acceptDeviceConfirmation() {
            LocalBroadcastManager.getInstance(SteamService.this)
                    .sendBroadcast(new Intent(DEVICE_CONFIRMATION_EVENT));
            return CompletableFuture.completedFuture(true);
        }
    }

    private CompletableFuture<String> requestGuardCode(EResult signal) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        pendingGuardCodeFuture = future;
        sendLoginResult(signal);
        return future;
    }

    private void cancelPendingGuardCode() {
        final CompletableFuture<String> future = pendingGuardCodeFuture;
        pendingGuardCodeFuture = null;
        if (future != null) {
            future.cancel(false);
        }
    }

    private void sendLoginResult(EResult result) {
        final Intent intent = new Intent(LOGIN_EVENT);
        intent.putExtra(RESULT, result);
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(intent);
    }

    private EResult resolveFailureResult(Exception e) {
        final Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
        if (cause instanceof AuthenticationException) {
            final EResult result = ((AuthenticationException) cause).getResult();
            if (result != null) {
                return result;
            }
        }
        return EResult.Fail;
    }

    private void registerApiKey() {
        Log.i(TAG, "Registering API key");
        final int result = webHandler.updateApiKey();
        Log.i(TAG, "API key result: " + result);
        switch (result) {
            case SteamWebHandler.ApiKeyState.REGISTERED:
                break;
            case SteamWebHandler.ApiKeyState.ACCESS_DENIED:
                showToast(getString(R.string.apikey_access_denied));
                break;
            case SteamWebHandler.ApiKeyState.UNREGISTERED:
                // Call updateApiKey once more to actually update it
                webHandler.updateApiKey();
                break;
            case SteamWebHandler.ApiKeyState.ERROR:
                showToast(getString(R.string.apikey_register_failed));
                break;
        }
    }

    private void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    private void onConnected(ConnectedCallback callback) {
        Log.i(TAG, "Connected()");
        connected = true;
        if (pendingQrLogin) {
            pendingQrLogin = false;
            doQrLogin();
        } else if (pendingAuthDetails != null) {
            doCredentialsLogin(pendingAuthDetails);
            pendingAuthDetails = null;
        } else {
            attemptRestoreLogin();
        }
    }

    private void onDisconnected(DisconnectedCallback callback) {
        Log.i(TAG, "Disconnected()");
        connected = false;
        loggedIn = false;

        if (!loginInProgress) {
            // Try to reconnect after a 5 second delay
            scheduler.schedule(() -> {
                Log.i(TAG, "Reconnecting");
                steamClient.connect();
            }, 5, TimeUnit.SECONDS);
        } else {
            // SteamKit may disconnect us while logging on (if already connected),
            // but since it reconnects immediately after we do not have to reconnect here.
            Log.i(TAG, "NOT reconnecting (logon in progress)");
        }

        // Tell the activity that we've been disconnected from Steam
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(new Intent(DISCONNECT_EVENT));
    }

    private void onLoggedOff(LoggedOffCallback callback) {
        Log.i(TAG, "Logoff result " + callback.getResult().toString());
        if (callback.getResult() == EResult.LoggedInElsewhere) {
            updateNotification(getString(R.string.logged_in_elsewhere));
            unscheduleFarmTask();
            if (!waiting) {
                waiting = true;
                waitHandle = scheduler.scheduleAtFixedRate(waitTask, 0, 30, TimeUnit.SECONDS);
            }
        } else {
            // Reconnect
            steamClient.disconnect();
        }
    }

    private void onLoggedOn(LoggedOnCallback callback) {
        final EResult result = callback.getResult();

        if (result == EResult.OK) {
            // Successful login
            Log.i(TAG, "Logged on!");
            loginInProgress = false;
            loggedIn = true;
            PrefsManager.writeRefreshToken(currentRefreshToken);
            final SteamID clientSteamId = steamClient.getSteamID();
            steamId = clientSteamId.convertToUInt64();
            if (paused) {
                showPausedNotification();
            } else if (waiting) {
                updateNotification(getString(R.string.logged_in_elsewhere));
            } else {
                updateNotification(getString(R.string.logged_in));
            }
            final String refreshToken = currentRefreshToken;
            executor.execute(() -> {
                final boolean gotAuth = attemptWebAuthentication(clientSteamId, refreshToken);

                if (gotAuth) {
                    resumeFarming();
                    registerApiKey();
                } else {
                    updateNotification(getString(R.string.web_login_failed));
                }
            });
            if (keyToRedeem != null) {
                redeemKey(keyToRedeem);
                keyToRedeem = null;
            }
        } else if (result == EResult.InvalidPassword && !PrefsManager.getRefreshToken().isEmpty()) {
            // Refresh token no longer valid
            Log.i(TAG, "Refresh token expired");
            PrefsManager.writeRefreshToken("");
            updateNotification(getString(R.string.login_key_expired));
            keyToRedeem = null;
            steamClient.disconnect();
        } else {
            Log.i(TAG, "LogOn result: " + result.toString());
            keyToRedeem = null;
            steamClient.disconnect();
        }

        PrefsManager.writeCellId(callback.getCellID());

        // Tell LoginActivity the result
        final Intent intent = new Intent(LOGIN_EVENT);
        intent.putExtra(RESULT, result);
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(intent);
    }

    private void onPurchaseResponse(PurchaseResponseCallback callback) {
        if (callback.getResult() == EResult.OK) {
            final KeyValue kv = callback.getPurchaseReceiptInfo();
            final EPaymentMethod paymentMethod = EPaymentMethod.from(kv.get("PaymentMethod").asInteger());
            if (paymentMethod == EPaymentMethod.ActivationCode) {
                final StringBuilder products = new StringBuilder();
                final int size = kv.get("LineItemCount").asInteger();
                Log.i(TAG, "LineItemCount " + size);
                for (int i = 0; i < size; i++) {
                    final String lineItem = kv.get("lineitems").get(i + "").get("ItemDescription").asString();
                    Log.i(TAG, "lineItem " + i + " " + lineItem);
                    products.append(lineItem);
                    if (i + 1 < size) {
                        products.append(", ");
                    }
                }
                showToast(getString(R.string.activated, products.toString()));
            }
        } else {
            final EPurchaseResultDetail purchaseResult = callback.getPurchaseResultDetails();
            final int errorId;
            if (purchaseResult == EPurchaseResultDetail.AlreadyPurchased) {
                errorId = R.string.product_already_owned;
            } else if (purchaseResult == EPurchaseResultDetail.BadActivationCode) {
                errorId = R.string.invalid_key;
            } else {
                errorId = R.string.activation_failed;
            }
            showToast(getString(errorId));
        }
    }

    private void onPersonaState(PersonaStateCallback callback) {
        if (callback.getFriendId().equals(steamClient.getSteamID())) {
            final String personaName = callback.getName();
            final String avatarHash = Utils.bytesToHex(callback.getAvatarHash()).toLowerCase();
            Log.i(TAG, "Avatar hash " + avatarHash);
            final Intent event = new Intent(PERSONA_EVENT);
            event.putExtra(PERSONA_NAME, personaName);
            event.putExtra(AVATAR_HASH, avatarHash);
            LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(event);
        }
    }

    private void onFreeLicense(FreeLicenseCallback callback) {
        final int freeLicense = pendingFreeLicenses.removeFirst();
        if (!callback.getGrantedApps().isEmpty()) {
            showToast(getString(R.string.activated, String.valueOf(callback.getGrantedApps().get(0))));
        } else if (!callback.getGrantedPackages().isEmpty()) {
            showToast(getString(R.string.activated, String.valueOf(callback.getGrantedPackages().get(0))));
        } else {
            // Try activating it with the web handler
            executor.execute(() -> {
                final String msg;
                if (webHandler.addFreeLicense(freeLicense)) {
                    msg = getString(R.string.activated, String.valueOf(freeLicense));
                } else {
                    msg = getString(R.string.activation_failed);
                }
                showToast(msg);
            });
        }
    }

    private void onAccountInfo(AccountInfoCallback callback) {
        if (!PrefsManager.getOffline()) {
            steamFriends.setPersonaState(EPersonaState.Online);
        }
    }

    private void onItemAnnouncements(ItemAnnouncementsCallback callback) {
        Log.i(TAG, "New item notification " + callback.getCount());
        if (callback.getCount() > 0 && farming) {
            // Possible card drop
            executor.execute(farmTask);
        }
    }

    /**
     * Idle one or more games
     *
     * @param games the games to idle
     */
    private void playGames(Game... games) {
        final ClientMsgProtobuf<SteammessagesClientserver.CMsgClientGamesPlayed.Builder> gamesPlayed;
        gamesPlayed = new ClientMsgProtobuf<>(SteammessagesClientserver.CMsgClientGamesPlayed.class, EMsg.ClientGamesPlayed);
        for (Game game : games) {
            if (game.appId == 0) {
                // Non-Steam game
                final GameID gameId = new GameID(game.appId);
                gameId.setAppType(GameID.GameType.SHORTCUT);
                final CRC32 crc = new CRC32();
                crc.update(game.name.getBytes());
                // set the high-bit on the mod-id
                // reduces crc32 to 31bits, but lets us use the modID as a guaranteed unique
                // replacement for appID
                gameId.setModID(crc.getValue() | (0x80000000));
                gamesPlayed.getBody().addGamesPlayedBuilder()
                        .setGameId(gameId.convertToUInt64())
                        .setGameExtraInfo(game.name);
            } else {
                gamesPlayed.getBody().addGamesPlayedBuilder()
                        .setGameId(game.appId);
            }
        }
        executor.execute(() -> {
            steamClient.send(gamesPlayed);
        });
        // Tell the activity
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(new Intent(NOW_PLAYING_EVENT));
    }

    private void stopPlaying() {
        if (!paused) {
            currentGames.clear();
        }
        final ClientMsgProtobuf<SteammessagesClientserver.CMsgClientGamesPlayed.Builder> stopGame;
        stopGame = new ClientMsgProtobuf<>(SteammessagesClientserver.CMsgClientGamesPlayed.class, EMsg.ClientGamesPlayed);
        stopGame.getBody().addGamesPlayedBuilder().setGameId(0);
        executor.execute(() -> steamClient.send(stopGame));
        // Tell the activity
        LocalBroadcastManager.getInstance(SteamService.this).sendBroadcast(new Intent(NOW_PLAYING_EVENT));
    }

    /**
     * Register and idle a game for a few seconds to complete the Spring Cleaning daily tasks
     */
    public void registerAndIdle(String game) {
        try {
            final int appId = Integer.parseInt(game);

            // Register the game
            steamApps.requestFreeLicense(appId);
            Thread.sleep(1000);

            // Play it for a few seconds
            final ClientMsgProtobuf<SteammessagesClientserver.CMsgClientGamesPlayed.Builder> playGame;
            playGame = new ClientMsgProtobuf<>(SteammessagesClientserver.CMsgClientGamesPlayed.class, EMsg.ClientGamesPlayed);
            playGame.getBody().addGamesPlayedBuilder().setGameId(appId);
            steamClient.send(playGame);
            Thread.sleep(3000);

            // Stop playing
            playGame.getBody().clearGamesPlayed().addGamesPlayedBuilder().setGameId(0);
            steamClient.send(playGame);
            Thread.sleep(1000);
        } catch (NumberFormatException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open a cottage door (Winter Sale 2018)
     */
    public void openCottageDoor() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = webHandler.openCottageDoor();
                if (result) {
                    showToast(getString(R.string.door_success));
                } else {
                    showToast(getString(R.string.door_fail));
                }
            }
        });
    }
}
