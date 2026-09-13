package com.gta.launcher.server;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

public class ServerInfo implements Serializable {
    private String ip;
    private int port;
    private String hostname;
    private int players;
    private int maxPlayers;
    private String gamemode;
    private String language;
    private boolean hasPassword;
    private String version;
    private int ping;
    private boolean isFavorite;
    private boolean isOnline;
    private long lastPlayedTimestamp;

    public ServerInfo() {
        this.ip = "127.0.0.1";
        this.port = 7777;
        this.hostname = "SA-MP Server";
        this.players = 0;
        this.maxPlayers = 100;
        this.gamemode = "Unknown";
        this.language = "English";
        this.hasPassword = false;
        this.version = "0.3.7";
        this.ping = -1;
        this.isFavorite = false;
        this.isOnline = true;
    }

    public ServerInfo(String ip, int port, String hostname, int players, int maxPlayers,
                      String gamemode, String language, boolean hasPassword, String version) {
        this.ip = ip;
        this.port = port;
        this.hostname = hostname != null ? hostname : "SA-MP Server";
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.gamemode = gamemode != null ? gamemode : "-";
        this.language = language != null ? language : "-";
        this.hasPassword = hasPassword;
        this.version = version != null ? version : "0.3.7";
        this.ping = -1;
        this.isFavorite = false;
        this.isOnline = true;
    }

    public static ServerInfo fromOpenMpJson(JSONObject obj) {
        ServerInfo info = new ServerInfo();
        try {
            String ipPort = obj.optString("ip", "");
            if (ipPort.contains(":")) {
                String[] parts = ipPort.split(":");
                info.setIp(parts[0]);
                try {
                    info.setPort(Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    info.setPort(7777);
                }
            } else {
                info.setIp(ipPort);
                info.setPort(7777);
            }
            info.setHostname(obj.optString("hn", "SA-MP Server"));
            info.setPlayers(obj.optInt("pc", 0));
            info.setMaxPlayers(obj.optInt("pm", 100));
            info.setGamemode(obj.optString("gm", "-"));
            info.setLanguage(obj.optString("la", "-"));
            info.setHasPassword(obj.optBoolean("pa", false));
            info.setVersion(obj.optString("vn", "0.3.7"));
            info.setOnline(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ip", ip);
            obj.put("port", port);
            obj.put("hostname", hostname);
            obj.put("players", players);
            obj.put("maxPlayers", maxPlayers);
            obj.put("gamemode", gamemode);
            obj.put("language", language);
            obj.put("hasPassword", hasPassword);
            obj.put("version", version);
            obj.put("ping", ping);
            obj.put("isFavorite", isFavorite);
            obj.put("lastPlayed", lastPlayedTimestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static ServerInfo fromJson(JSONObject obj) {
        ServerInfo info = new ServerInfo();
        info.setIp(obj.optString("ip", "127.0.0.1"));
        info.setPort(obj.optInt("port", 7777));
        info.setHostname(obj.optString("hostname", "SA-MP Server"));
        info.setPlayers(obj.optInt("players", 0));
        info.setMaxPlayers(obj.optInt("maxPlayers", 100));
        info.setGamemode(obj.optString("gamemode", "-"));
        info.setLanguage(obj.optString("language", "-"));
        info.setHasPassword(obj.optBoolean("hasPassword", false));
        info.setVersion(obj.optString("version", "0.3.7"));
        info.setPing(obj.optInt("ping", -1));
        info.setFavorite(obj.optBoolean("isFavorite", false));
        info.setLastPlayedTimestamp(obj.optLong("lastPlayed", 0));
        return info;
    }

    public String getAddress() {
        return ip + ":" + port;
    }

    public String getCleanHostname() {
        if (hostname == null) return "SA-MP Server";
        // Strip SA-MP color brackets e.g. {FFFFFF} or {33CCFF}
        return hostname.replaceAll("\\{[A-Fa-f0-9]{6}\\}", "").trim();
    }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public int getPlayers() { return players; }
    public void setPlayers(int players) { this.players = players; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public String getGamemode() { return gamemode; }
    public void setGamemode(String gamemode) { this.gamemode = gamemode; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getPing() { return ping; }
    public void setPing(int ping) { this.ping = ping; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public long getLastPlayedTimestamp() { return lastPlayedTimestamp; }
    public void setLastPlayedTimestamp(long lastPlayedTimestamp) { this.lastPlayedTimestamp = lastPlayedTimestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerInfo that = (ServerInfo) o;
        return port == that.port && Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
