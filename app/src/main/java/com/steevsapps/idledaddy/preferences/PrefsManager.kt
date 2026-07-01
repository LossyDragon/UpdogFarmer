package com.steevsapps.idledaddy.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.steevsapps.idledaddy.steam.model.Game;
import com.steevsapps.idledaddy.utils.CryptHelper;
import com.steevsapps.idledaddy.utils.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SharedPreferences manager
 */
public class PrefsManager {
    private final static int CURRENT_VERSION = 3;

    private final static String USERNAME = "username";
    private final static String PASSWORD = "password";
    private final static String REFRESH_TOKEN = "login_key";
    private final static String GUARD_DATA = "sentry_hash";
    private final static String SHARED_SECRET = "shared_secret";
    private final static String OFFLINE = "offline";
    private final static String STAY_AWAKE = "stay_awake";
    private final static String MINIMIZE_DATA = "minimize_data";
    private final static String PARENTAL_PIN = "parental_pin";
    private final static String BLACKLIST = "blacklist";
    private final static String LAST_SESSION = "last_session";
    private final static String HOURS_UNTIL_DROPS = "hours_until_drops";
    private final static String INCLUDE_FREE_GAMES = "include_free_games";
    private final static String USE_CUSTOM_LOGINID = "use_custom_loginid";
    private final static String PERSONA_NAME = "persona_name";
    private final static String AVATAR_HASH = "avatar_hash";
    private final static String API_KEY = "api_key";
    private final static String LANGUAGE = "language";
    private final static String VERSION = "version";
    private final static String SORT_VALUE = "sort_value";
    private final static String CELL_ID = "cell_id";

    private static SharedPreferences prefs;

    private PrefsManager() {
    }

    public static void init(Context c) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(c);
        }

        if (getVersion() != CURRENT_VERSION) {
            onUpgrade(getVersion());
        }
    }

    private static void onUpgrade(int oldVersion) {
        if (oldVersion < 2) {
            // Serialized names have changed
            writeLastSession(new ArrayList<>());
        }
        if (oldVersion < 3) {
            writeRefreshToken("");
            writeGuardData("");
        }
        writeVersion(CURRENT_VERSION);
    }

    /**
     * Clear all preferences related to user
     */
    public static void clearUser() {
        prefs.edit()
                .putString(USERNAME, "")
                .putString(PASSWORD, "")
                .putString(REFRESH_TOKEN, "")
                .putString(GUARD_DATA, "")
                .putString(BLACKLIST, "")
                .putString(LAST_SESSION, "")
                .putString(PARENTAL_PIN, "")
                .putString(PERSONA_NAME, "")
                .putString(AVATAR_HASH, "")
                .putString(API_KEY, "")
                .apply();
    }

    public static SharedPreferences getPrefs() {
        return prefs;
    }

    public static void writeUsername(String username) {
        writePref(USERNAME, username);
    }

    public static void writePassword(Context context, String password) {
        writePref(PASSWORD, CryptHelper.encryptString(context, password));
    }

    public static void writeRefreshToken(String refreshToken) {
        writePref(REFRESH_TOKEN, refreshToken);
    }

    public static void writeGuardData(String guardData) {
        writePref(GUARD_DATA, guardData);
    }

    public static void writeSharedSecret(String sharedSecret) {
        writePref(SHARED_SECRET, sharedSecret);
    }

    public static void writeBlacklist(List<String> blacklist) {
        writePref(BLACKLIST, Utils.arrayToString(blacklist));
    }

    public static void writeLastSession(List<Game> games) {
        final String json = new Gson().toJson(games);
        writePref(LAST_SESSION, json);
    }

    public static void writePersonaName(String personaName) {
        writePref(PERSONA_NAME, personaName);
    }

    public static void writeAvatarHash(String avatarHash) {
        writePref(AVATAR_HASH, avatarHash);
    }

    public static void writeApiKey(String apiKey) {
        writePref(API_KEY, apiKey);
    }

    public static void writeLanguage(String language) {
        writePref(LANGUAGE, language);
    }

    public static void writeVersion(int version) {
        writePref(VERSION, version);
    }

    public static void writeSortValue(int sortValue) {
        writePref(SORT_VALUE, sortValue);
    }

    public static String getUsername() {
        return prefs.getString(USERNAME, "");
    }

    public static String getPassword(Context context) {
        return CryptHelper.decryptString(context, prefs.getString(PASSWORD, ""));
    }

    public static String getRefreshToken() {
        return prefs.getString(REFRESH_TOKEN, "");
    }

    public static String getGuardData() {
        return prefs.getString(GUARD_DATA, "");
    }

    public static String getSharedSecret() {
        return prefs.getString(SHARED_SECRET, "");
    }

    public static boolean getOffline() {
        return prefs.getBoolean(OFFLINE, false);
    }

    public static boolean stayAwake() {
        return prefs.getBoolean(STAY_AWAKE, false);
    }

    public static boolean minimizeData() {
        return prefs.getBoolean(MINIMIZE_DATA, false);
    }

    public static String getParentalPin() {
        return prefs.getString(PARENTAL_PIN, "");
    }

    public static List<String> getBlacklist() {
        final String[] blacklist = prefs.getString(BLACKLIST, "").split(",");
        return new ArrayList<>(Arrays.asList(blacklist));
    }

    public static List<Game> getLastSession() {
        final String json = prefs.getString(LAST_SESSION, "");
        final Type type = new TypeToken<List<Game>>(){}.getType();
        final List<Game> games = new Gson().fromJson(json, type);
        if (games == null) {
            return new ArrayList<>();
        }
        return games;
    }

    public static String getPersonaName() {
        return prefs.getString(PERSONA_NAME, "");
    }

    public static String getAvatarHash() {
        return prefs.getString(AVATAR_HASH, "");
    }

    public static int getHoursUntilDrops() {
        return prefs.getInt(HOURS_UNTIL_DROPS, 3);
    }

    public static boolean includeFreeGames() {
        return prefs.getBoolean(INCLUDE_FREE_GAMES, false);
    }

    public static boolean useCustomLoginId() {
        return prefs.getBoolean(USE_CUSTOM_LOGINID, false);
    }

    public static String getApiKey() {
        return prefs.getString(API_KEY, "");
    }

    public static String getLanguage() {
        return prefs.getString(LANGUAGE, "");
    }

    public static int getVersion() {
        return prefs.getInt(VERSION, 1);
    }

    public static int getSortValue() {
        return prefs.getInt(SORT_VALUE, 0);
    }

    public static int getCellId() {
        return prefs.getInt(CELL_ID, -1);
    }

    public static void writeCellId(Integer cellId) {
        writePref(CELL_ID, cellId);
    }

    private static void writePref(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    private static void writePref(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }
}
