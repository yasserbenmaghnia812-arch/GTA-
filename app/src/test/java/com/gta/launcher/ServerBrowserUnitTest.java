package com.gta.launcher;

import com.gta.launcher.server.ServerInfo;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServerBrowserUnitTest {

    @Test
    public void testServerInfoCleanHostname() {
        ServerInfo info = new ServerInfo();
        info.setHostname("{FF0000}[ENG] {00FF00}Horizon {33CCFF}Roleplay");
        assertEquals("[ENG] Horizon Roleplay", info.getCleanHostname());
    }

    @Test
    public void testServerInfoJsonSerialization() {
        ServerInfo server = new ServerInfo("185.169.134.157", 7777, "Advance RP", 125, 1000,
                "RolePlay", "Russian", false, "0.3.7-R2");
        server.setPing(45);
        server.setFavorite(true);

        JSONObject json = server.toJson();
        assertNotNull(json);

        ServerInfo restored = ServerInfo.fromJson(json);
        assertEquals("185.169.134.157", restored.getIp());
        assertEquals(7777, restored.getPort());
        assertEquals("185.169.134.157:7777", restored.getAddress());
        assertEquals("Advance RP", restored.getCleanHostname());
        assertEquals(125, restored.getPlayers());
        assertEquals(1000, restored.getMaxPlayers());
        assertEquals("RolePlay", restored.getGamemode());
        assertEquals(45, restored.getPing());
        assertTrue(restored.isFavorite());
        assertFalse(restored.isHasPassword());
    }

    @Test
    public void testServerInfoFromOpenMpJson() throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("ip", "51.254.181.189:7777");
        obj.put("hn", "{FFFFFF}Ultra Stunt & Drift");
        obj.put("pc", 42);
        obj.put("pm", 200);
        obj.put("gm", "Stunt");
        obj.put("la", "English");
        obj.put("pa", true);
        obj.put("vn", "0.3.7");

        ServerInfo info = ServerInfo.fromOpenMpJson(obj);
        assertEquals("51.254.181.189", info.getIp());
        assertEquals(7777, info.getPort());
        assertEquals("Ultra Stunt & Drift", info.getCleanHostname());
        assertEquals(42, info.getPlayers());
        assertEquals(200, info.getMaxPlayers());
        assertTrue(info.isHasPassword());
    }

    @Test
    public void testServerEquality() {
        ServerInfo s1 = new ServerInfo("127.0.0.1", 7777, "Server 1", 0, 100, "-", "-", false, "0.3.7");
        ServerInfo s2 = new ServerInfo("127.0.0.1", 7777, "Renamed Server", 50, 100, "RP", "EN", true, "0.3.7");
        ServerInfo s3 = new ServerInfo("127.0.0.1", 7778, "Different Port", 0, 100, "-", "-", false, "0.3.7");

        assertEquals(s1, s2);
        assertFalse(s1.equals(s3));
    }
}
