# YASSER GAME STUDIO — Multiplayer and Android Validation

This checklist is the acceptance gate before merging the draft pull request or calling an APK stable.

## Two-device LAN multiplayer

1. Install the same APK build on two Android devices.
2. Connect both devices to the same Wi-Fi or phone hotspot.
3. On device A, press **HOST 8** and confirm the hosting status.
4. Find device A's local IPv4 address in Android network details.
5. On device B, enter that address and press **JOIN**.
6. Walk and drive around on both devices for at least five minutes.
7. Confirm each remote player appears, moves smoothly, and disappears after disconnect.
8. Repeat with one device entering and exiting a vehicle.
9. Test reconnect after closing and reopening the client.
10. Confirm the game reports a clear connection failure when an invalid address is used.

## Multiplayer acceptance limits

- Host plus seven clients is the configured maximum.
- ENet uses UDP port 7777; the network must allow peer-to-peer traffic.
- Current replication covers remote player transforms only. It does not yet replicate vehicle physics, weapon damage, health, police/NPC state, mission state, or authoritative inventory.
- Client-reported movement is range/speed checked by the host, but this is not a cheat-proof authoritative server.
- Do not expose a public internet server until authentication, authoritative simulation, rate limiting, and abuse handling are implemented.

## Android / Galaxy A06 acceptance

- Export and install the ARM64 debug APK.
- Verify launch, touch movement, camera drag, vehicle entry/exit, vehicle tuning, firing, reload, health, wanted level, missions, and save persistence.
- Run for 15 minutes and check for crashes, overheating, severe frame drops, and memory pressure.
- Test with other apps recently used so Android memory pressure is realistic.
- Keep the 30 FPS cap, Compatibility renderer, no real-time shadows, and low-poly procedural assets unless device testing demonstrates headroom.

## Release gate

A build is accepted only when the GitHub Actions export succeeds, the APK signature verifies, the artifact downloads, and the APK installs and runs on a physical Android device. A successful CI export alone does not prove gameplay or multiplayer correctness.
