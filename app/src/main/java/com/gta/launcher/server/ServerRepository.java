package com.gta.launcher.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerRepository {
    private static final String TAG = "ServerRepository";
    private static final String OPEN_MP_API = "https://api.open.mp/servers";
    private static final String PREFS_SERVERS = "samp_servers_prefs";
    private static final String KEY_FAVORITES = "favorite_servers";
    private static final String KEY_RECENTS = "recent_servers";

    private final Context context;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private List<ServerInfo> cachedInternetServers = new ArrayList<>();
    private List<ServerInfo> cachedLocalServers = new ArrayList<>();

    public interface ServerListCallback {
        void onLoaded(List<ServerInfo> servers);
        void onError(String message);
    }

    public ServerRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void fetchInternetServers(final boolean forceRefresh, final ServerListCallback callback) {
        if (!forceRefresh && !cachedInternetServers.isEmpty()) {
            syncFavorites(cachedInternetServers);
            callback.onLoaded(new ArrayList<>(cachedInternetServers));
            return;
        }

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(OPEN_MP_API);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SAMP-Client/2.11");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONArray jsonArray = new JSONArray(sb.toString());
                    List<ServerInfo> servers = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        ServerInfo info = ServerInfo.fromOpenMpJson(obj);
                        servers.add(info);
                    }

                    syncFavorites(servers);
                    cachedInternetServers = servers;

                    mainHandler.post(() -> callback.onLoaded(new ArrayList<>(servers)));
                    return;
                } else {
                    throw new Exception("HTTP " + responseCode);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed fetching online servers: " + e.getMessage() + ", falling back to local list");
                // Load local fallback servers if internet request failed
                List<ServerInfo> fallback = loadLocalDefaultServers();
                syncFavorites(fallback);
                mainHandler.post(() -> {
                    if (fallback.isEmpty()) {
                        callback.onError("Failed to connect to server masterlist: " + e.getMessage());
                    } else {
                        callback.onLoaded(fallback);
                    }
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    public List<ServerInfo> loadLocalDefaultServers() {
        if (!cachedLocalServers.isEmpty()) {
            syncFavorites(cachedLocalServers);
            return new ArrayList<>(cachedLocalServers);
        }

        List<ServerInfo> list = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("default_servers.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(ServerInfo.fromOpenMpJson(obj));
            }
            cachedLocalServers = list;
            syncFavorites(list);
        } catch (Exception e) {
            Log.e(TAG, "Error loading default servers from assets: " + e.getMessage());
        }
        return list;
    }

    public List<ServerInfo> getFavorites() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SERVERS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_FAVORITES, "[]");
        List<ServerInfo> favorites = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                ServerInfo s = ServerInfo.fromJson(array.getJSONObject(i));
                s.setFavorite(true);
                favorites.add(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites: " + e.getMessage());
        }
        return favorites;
    }

    public void addFavorite(ServerInfo server) {
        if (server == null) return;
        List<ServerInfo> favorites = getFavorites();
        for (ServerInfo existing : favorites) {
            if (existing.equals(server)) {
                return;
            }
        }
        server.setFavorite(true);
        favorites.add(server);
        saveFavorites(favorites);
        syncFavorites(cachedInternetServers);
    }

    public void removeFavorite(ServerInfo server) {
        if (server == null) return;
        List<ServerInfo> favorites = getFavorites();
        favorites.removeIf(s -> s.equals(server));
        server.setFavorite(false);
        saveFavorites(favorites);
        syncFavorites(cachedInternetServers);
    }

    public boolean isFavorite(ServerInfo server) {
        if (server == null) return false;
        List<ServerInfo> favorites = getFavorites();
        for (ServerInfo s : favorites) {
            if (s.equals(server)) return true;
        }
        return false;
    }

    private void saveFavorites(List<ServerInfo> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SERVERS, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (ServerInfo s : list) {
            array.put(s.toJson());
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply();
    }

    public List<ServerInfo> getRecents() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SERVERS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECENTS, "[]");
        List<ServerInfo> recents = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                ServerInfo s = ServerInfo.fromJson(array.getJSONObject(i));
                recents.add(s);
            }
            syncFavorites(recents);
        } catch (Exception e) {
            Log.e(TAG, "Error loading recents: " + e.getMessage());
        }
        return recents;
    }

    public void addRecent(ServerInfo server) {
        if (server == null) return;
        List<ServerInfo> recents = getRecents();
        recents.removeIf(s -> s.equals(server));
        server.setLastPlayedTimestamp(System.currentTimeMillis());
        recents.add(0, server);
        while (recents.size() > 20) {
            recents.remove(recents.size() - 1);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_SERVERS, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (ServerInfo s : recents) {
            array.put(s.toJson());
        }
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply();
    }

    private void syncFavorites(List<ServerInfo> servers) {
        if (servers == null) return;
        Set<String> favAddresses = new HashSet<>();
        for (ServerInfo f : getFavorites()) {
            favAddresses.add(f.getAddress());
        }
        for (ServerInfo s : servers) {
            s.setFavorite(favAddresses.contains(s.getAddress()));
        }
    }
}
