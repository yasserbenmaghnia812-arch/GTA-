# YASSER GAME STUDIO — Open World

An original 3D Android game foundation built with Godot 4.7.2. The first playable slice focuses on a procedural low-poly city, third-person movement, an arcade vehicle, target practice, mobile touch controls, and an 8-player ENet connection prototype.

## Current vertical slice

- Procedural city blocks and roads generated at runtime.
- Third-person character movement, sprinting, jumping, and camera look.
- Drivable four-wheel arcade vehicle; press **E** to enter or exit.
- Simple target-practice ray shooting; press **F**.
- Mobile touch D-pad and action buttons.
- Host / Join using ENet UDP port **7777**, with a maximum of 8 peers.
- Android export preset for **ARM64** and internet permission.
- GitHub Actions workflow for producing a debug APK artifact.

## Controls

| Action | Keyboard | Touch |
|---|---|---|
| Move | WASD | D-pad |
| Sprint | Shift | RUN |
| Jump | Space | JUMP |
| Enter / exit car | E | ENTER / EXIT |
| Fire | F | FIRE |
| Drive / steer | WASD | D-pad |
| Brake | Space | BRAKE |
| Look | Mouse | Drag on the right half of the screen |

For a LAN test, start **HOST** on one device and enter that device's local IP on the other device, then press **JOIN**. Both devices must be on a network that allows device-to-device traffic. Internet play between different networks needs a reachable server or router UDP forwarding for port 7777. This prototype uses client-reported transforms; it is not yet a production authoritative anti-cheat server.

## Build an Android APK

The repository workflow is configured to build the game with Godot 4.7.2 and upload **YASSER-OPEN-WORLD-APK** as a GitHub Actions artifact.

To build locally:

1. Install Godot 4.7.2 Standard and its matching export templates.
2. Install OpenJDK 17 and Android SDK Platform 35, Build Tools 35.0.1, NDK 28.1.13356709, and CMake 3.10.2.4988404.
3. Open this folder as a Godot project.
4. Run **Project → Export → Android**.

The generated package ID is **com.yasser.openworld**.

## Performance targets

The initial profile uses Godot's Compatibility renderer, primitive meshes, no real-time shadows, a small procedural map, and ARM64-only export. These are starting constraints for a 4 GB Android device, not a claim of measured frame rate. Test the resulting APK on the target phone before increasing map density, NPC count, textures, or effects.

## Roadmap

1. Replace primitive placeholders with licensed low-poly characters, vehicles, props, and animations.
2. Add traffic AI, pedestrians, mission scripting, map/garage/economy systems, and save data.
3. Add server-authoritative multiplayer, lobby flow, reconnection, and dedicated server deployment.
4. Add FPS/TPS loadouts, recoil, aiming, weapon switching, team modes, and match flow.
5. Add a separate football/sports mode using original ball physics, team tactics, player switching, and match rules.

## Inspirations and licensing

The game uses genre-level design references from open-world driving games, tactical shooters, and football simulations. It does **not** include GTA, Call of Duty, FIFA, Rockstar, Activision, or EA source code, characters, models, maps, music, logos, or other proprietary assets.

Godot Engine is distributed under the MIT license; see https://godotengine.org/license/. The original code in this folder is MIT licensed. Any third-party asset added later must have its license and attribution recorded in **THIRD_PARTY_NOTICES.md** before distribution.

OpenCity (https://github.com/webgl-games/opencity) was reviewed as a reference for procedural city/vehicle gameplay; its code/assets have not been copied into this starter.
