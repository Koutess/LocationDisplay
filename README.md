# Location Display

Client-side Fabric mod: private markers drawn as a thin vertical line with name, icon and distance.
Nothing is sent to the server, so it works on any server.

**Key:** `\` opens the menu, `Shift + \` toggles markers on/off (rebindable in Controls).

## Supported versions: Minecraft 1.21 – 1.21.11 (Fabric)

CI builds **one jar per Minecraft version** (`location-display-mc1.21.4-1.1.0.jar`, etc. — all 12 of them, also bundled
as `location-display-all-1.21.x`). Just download the one matching your game.

Under the hood, Minecraft/Fabric API changed in ways that can't be covered by one code path, so the source has three compat sets:

| Compat set            | Minecraft            | Why it's separate                          |
|-----------------------|----------------------|--------------------------------------------|
| `mc1.21-1.21.5`       | 1.21 – 1.21.5        | `HudRenderCallback`, string keybind category |
| `mc1.21.6-1.21.8`     | 1.21.6 – 1.21.8      | new `HudElementRegistry`                   |
| `mc1.21.9-1.21.11`    | 1.21.9 – 1.21.11     | `KeyBinding.Category` objects              |

All three share the same code; only `src/compat/<set>/…/Compat.java` differs. Requires Java 21, Fabric Loader ≥ 0.16, Fabric API.

## Building

```bash
# default (1.21.11):
gradle build

# any other version – the script looks up Yarn / Loader / Fabric API versions from Fabric's metadata:
gradle build $(scripts/resolve-versions.sh 1.21.4)
```
The right compat set is chosen automatically from `minecraft_version`. The jar is named after the exact version it was built for.
