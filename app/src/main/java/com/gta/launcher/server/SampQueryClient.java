package com.gta.launcher.server;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SampQueryClient {
    private static final String TAG = "SampQueryClient";
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface QueryCallback {
        void onSuccess(ServerInfo serverInfo);
        void onError(Exception e);
    }

    public static void queryServer(final String host, final int port, final QueryCallback callback) {
        executor.execute(() -> {
            DatagramSocket socket = null;
            try {
                InetAddress address = InetAddress.getByName(host);
                byte[] ipBytes = address.getAddress();

                byte[] packetData = new byte[11];
                packetData[0] = 'S';
                packetData[1] = 'A';
                packetData[2] = 'M';
                packetData[3] = 'P';
                packetData[4] = ipBytes[0];
                packetData[5] = ipBytes[1];
                packetData[6] = ipBytes[2];
                packetData[7] = ipBytes[3];
                packetData[8] = (byte) (port & 0xFF);
                packetData[9] = (byte) ((port >> 8) & 0xFF);
                packetData[10] = 'i'; // Info opcode

                socket = new DatagramSocket();
                socket.setSoTimeout(2000);

                DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, address, port);
                long startTime = System.currentTimeMillis();
                socket.send(sendPacket);

                byte[] buffer = new byte[2048];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);
                long ping = System.currentTimeMillis() - startTime;

                if (receivePacket.getLength() > 11) {
                    ByteBuffer bb = ByteBuffer.wrap(buffer, 11, receivePacket.getLength() - 11);
                    bb.order(ByteOrder.LITTLE_ENDIAN);

                    boolean hasPassword = bb.get() != 0;
                    int players = bb.getShort() & 0xFFFF;
                    int maxPlayers = bb.getShort() & 0xFFFF;

                    int hnLen = bb.getInt();
                    if (hnLen < 0 || hnLen > bb.remaining()) hnLen = Math.min(Math.max(0, hnLen), bb.remaining());
                    byte[] hnBytes = new byte[hnLen];
                    bb.get(hnBytes);
                    String hostname = new String(hnBytes, StandardCharsets.UTF_8);

                    int gmLen = bb.getInt();
                    if (gmLen < 0 || gmLen > bb.remaining()) gmLen = Math.min(Math.max(0, gmLen), bb.remaining());
                    byte[] gmBytes = new byte[gmLen];
                    bb.get(gmBytes);
                    String gamemode = new String(gmBytes, StandardCharsets.UTF_8);

                    int laLen = bb.getInt();
                    if (laLen < 0 || laLen > bb.remaining()) laLen = Math.min(Math.max(0, laLen), bb.remaining());
                    byte[] laBytes = new byte[laLen];
                    bb.get(laBytes);
                    String language = new String(laBytes, StandardCharsets.UTF_8);

                    ServerInfo serverInfo = new ServerInfo(host, port, hostname, players, maxPlayers,
                            gamemode, language, hasPassword, "0.3.7");
                    serverInfo.setPing((int) ping);
                    serverInfo.setOnline(true);

                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(serverInfo);
                    });
                    return;
                }
                throw new Exception("Invalid response length");

            } catch (Exception e) {
                Log.w(TAG, "Query failed for " + host + ":" + port + " -> " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        });
    }
}
