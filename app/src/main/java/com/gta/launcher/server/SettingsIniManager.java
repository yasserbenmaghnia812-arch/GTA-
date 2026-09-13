package com.gta.launcher.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsIniManager {
    private static final String TAG = "SettingsIniManager";
    private static final String PREFS_NAME = "samp_launcher_prefs";
    private static final String KEY_NICKNAME = "player_nickname";
    private static final String KEY_LAST_HOST = "last_host";
    private static final String KEY_LAST_PORT = "last_port";
    private static final String KEY_LAST_HOSTNAME = "last_hostname";
    private static final String KEY_LAST_PASSWORD = "last_password";

    public static final String SETTINGS_PATH = "/storage/emulated/0/GTA/SAMP/settings.ini";

    public static String getPlayerNickname(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String defaultNick = "Player_" + (int)(Math.random() * 9000 + 1000);
        String saved = prefs.getString(KEY_NICKNAME, null);
        if (saved != null && !saved.trim().isEmpty()) {
            return saved;
        }

        // Try reading from settings.ini
        try {
            File iniFile = new File(SETTINGS_PATH);
            if (iniFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(iniFile));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.toLowerCase().startsWith("name=")) {
                        String name = line.substring(5).trim();
                        if (!name.isEmpty()) {
                            prefs.edit().putString(KEY_NICKNAME, name).apply();
                            br.close();
                            return name;
                        }
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed reading nick from ini: " + e.getMessage());
        }

        prefs.edit().putString(KEY_NICKNAME, defaultNick).apply();
        return defaultNick;
    }

    public static void setPlayerNickname(Context context, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) return;
        String clean = nickname.trim().replaceAll("\\s+", "_");
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NICKNAME, clean)
                .apply();

        // Also update settings.ini if it exists
        try {
            File iniFile = new File(SETTINGS_PATH);
            if (iniFile.exists()) {
                updateClientProperty("name", clean);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to update nick in ini: " + e.getMessage());
        }
    }

    public static ServerInfo getCurrentConfiguredServer(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String host = prefs.getString(KEY_LAST_HOST, null);
        int port = prefs.getInt(KEY_LAST_PORT, 7777);
        String hn = prefs.getString(KEY_LAST_HOSTNAME, "SA-MP Server");

        if (host != null && !host.isEmpty()) {
            ServerInfo info = new ServerInfo();
            info.setIp(host);
            info.setPort(port);
            info.setHostname(hn);
            return info;
        }

        // Check ini file directly
        try {
            File iniFile = new File(SETTINGS_PATH);
            if (iniFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(iniFile));
                String line;
                String iniHost = null;
                int iniPort = 7777;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.toLowerCase().startsWith("host=")) {
                        iniHost = line.substring(5).trim();
                    } else if (line.toLowerCase().startsWith("port=")) {
                        try {
                            iniPort = Integer.parseInt(line.substring(5).trim());
                        } catch (Exception ignored) {}
                    }
                }
                br.close();
                if (iniHost != null && !iniHost.isEmpty()) {
                    ServerInfo info = new ServerInfo();
                    info.setIp(iniHost);
                    info.setPort(iniPort);
                    info.setHostname("Configured Server");
                    return info;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed reading server from ini: " + e.getMessage());
        }

        return null;
    }

    public static boolean writeServerConnection(Context context, ServerInfo server, String nickname, String password) {
        if (server == null) return false;

        String host = server.getIp();
        int port = server.getPort();
        String pass = password != null ? password : "";
        String nick = (nickname != null && !nickname.trim().isEmpty()) ? nickname.trim() : getPlayerNickname(context);

        // Save to SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LAST_HOST, host)
                .putInt(KEY_LAST_PORT, port)
                .putString(KEY_LAST_HOSTNAME, server.getCleanHostname())
                .putString(KEY_LAST_PASSWORD, pass)
                .putString(KEY_NICKNAME, nick)
                .apply();

        // Write to /storage/emulated/0/GTA/SAMP/settings.ini
        try {
            File iniFile = new File(SETTINGS_PATH);
            File parentDir = iniFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Map<String, Map<String, String>> iniData = loadExistingIni(iniFile);

            // Ensure sections exist
            Map<String, String> clientSection = iniData.computeIfAbsent("client", k -> new LinkedHashMap<>());
            clientSection.put("name", nick);
            clientSection.put("host", host);
            clientSection.put("port", String.valueOf(port));
            clientSection.put("password", pass);
            if (!clientSection.containsKey("version")) clientSection.put("version", "0.3.7");
            if (!clientSection.containsKey("autoaim")) clientSection.put("autoaim", "false");

            Map<String, String> debugSection = iniData.computeIfAbsent("debug", k -> new LinkedHashMap<>());
            if (!debugSection.containsKey("debug")) debugSection.put("debug", "false");
            if (!debugSection.containsKey("online")) debugSection.put("online", "true");

            Map<String, String> guiSection = iniData.computeIfAbsent("gui", k -> new LinkedHashMap<>());
            if (!guiSection.containsKey("Font")) guiSection.put("Font", "arial.ttf");
            if (!guiSection.containsKey("FontSize")) guiSection.put("FontSize", "30.000000");
            if (!guiSection.containsKey("FontOutline")) guiSection.put("FontOutline", "2");
            if (!guiSection.containsKey("ChatPosX")) guiSection.put("ChatPosX", "325.000000");
            if (!guiSection.containsKey("ChatPosY")) guiSection.put("ChatPosY", "25.000000");
            if (!guiSection.containsKey("ChatSizeX")) guiSection.put("ChatSizeX", "1150.000000");
            if (!guiSection.containsKey("ChatSizeY")) guiSection.put("ChatSizeY", "220.000000");
            if (!guiSection.containsKey("ChatMaxMessages")) guiSection.put("ChatMaxMessages", "6");
            if (!guiSection.containsKey("Dialog")) guiSection.put("Dialog", "true");
            if (!guiSection.containsKey("FPSLimit")) guiSection.put("FPSLimit", "60");
            if (!guiSection.containsKey("fps")) guiSection.put("fps", "false");
            if (!guiSection.containsKey("firstperson")) guiSection.put("firstperson", "true");

            // Write out file
            PrintWriter pw = new PrintWriter(new FileWriter(iniFile));
            for (Map.Entry<String, Map<String, String>> sectionEntry : iniData.entrySet()) {
                pw.println("[" + sectionEntry.getKey() + "]");
                for (Map.Entry<String, String> valEntry : sectionEntry.getValue().entrySet()) {
                    pw.println(valEntry.getKey() + "=" + valEntry.getValue());
                }
                pw.println();
            }
            pw.flush();
            pw.close();

            Log.i(TAG, "Successfully wrote server config to settings.ini: " + host + ":" + port + " (" + nick + ")");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error writing settings.ini: " + e.getMessage(), e);
            return false;
        }
    }

    private static void updateClientProperty(String key, String value) {
        try {
            File iniFile = new File(SETTINGS_PATH);
            if (!iniFile.exists()) return;

            Map<String, Map<String, String>> iniData = loadExistingIni(iniFile);
            Map<String, String> clientSection = iniData.computeIfAbsent("client", k -> new LinkedHashMap<>());
            clientSection.put(key, value);

            PrintWriter pw = new PrintWriter(new FileWriter(iniFile));
            for (Map.Entry<String, Map<String, String>> sectionEntry : iniData.entrySet()) {
                pw.println("[" + sectionEntry.getKey() + "]");
                for (Map.Entry<String, String> valEntry : sectionEntry.getValue().entrySet()) {
                    pw.println(valEntry.getKey() + "=" + valEntry.getValue());
                }
                pw.println();
            }
            pw.flush();
            pw.close();
        } catch (Exception e) {
            Log.e(TAG, "Error updating ini property: " + e.getMessage());
        }
    }

    private static Map<String, Map<String, String>> loadExistingIni(File iniFile) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        if (!iniFile.exists()) return result;

        try {
            BufferedReader br = new BufferedReader(new FileReader(iniFile));
            String line;
            String currentSection = "client";

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim();
                } else if (line.contains("=")) {
                    int eqIdx = line.indexOf('=');
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    result.computeIfAbsent(currentSection, k -> new LinkedHashMap<>()).put(key, val);
                }
            }
            br.close();
        } catch (Exception e) {
            Log.w(TAG, "Failed reading existing ini: " + e.getMessage());
        }
        return result;
    }
}
