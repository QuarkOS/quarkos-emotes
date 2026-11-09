# QuarkOS Emotes

Expressive player animations for Minecraft, by **Emilio Schwaiger / QuarkOS**.
Mod ID: `quarkos_emotes`. Version: **1.0.0**. License: **MIT**.

This branch supplies the initial emote content and documentation. The runtime
is developed in parallel; a successful four-target build and in-game review
are still required before this is a verified release.

## Included animations

Sixteen original, numeric Bedrock/Blockbench `.animation.json` files live in
[`assets/quarkos_emotes/emotes`](src/main/resources/assets/quarkos_emotes/emotes/).

| Emote | Length | Playback |
| --- | --- | --- |
| Wave | 2.2 s | Once |
| Thinking | 3 s | Loop |
| Facepalm | 2.8 s | Once |
| Table Flip | 3.4 s | Hold final pose |
| Clap | 1.2 s | Loop; two timed vanilla slap cues |
| Bow | 2.6 s | Once |
| Salute | 2.1 s | Once |
| Laugh | 2 s | Loop |
| Dab | 2.1 s | Once |
| Sit | 2.2 s | Hold final pose |
| Groove | 2 s | Loop |
| Spotlight Dance | 8 s | Loop; steps, arm waves, full turn, hip sway |
| Shrug | 2.4 s | Once |
| Point | 2.4 s | Once |
| Cheer | 2 s | Loop; visual hop and raised arms |
| Stretch | 3.2 s | Once |

The table prop is authored from five cubes using the vanilla oak-plank
texture. Sounds reference vanilla sound events; no audio or texture files
are bundled here. These are initial Steve-rig animations pending visual
review and possible replacement by the Blockbench animator's exports.

## Target compatibility

| Loader | Minecraft | Target loader build |
| --- | --- | --- |
| Fabric | 1.21.1 | Determined by runtime build |
| Fabric | 1.21.11 | Determined by runtime build |
| Forge | 1.21.1 | 52.1.20 |
| Forge | 1.21.11 | 61.2.4 |

The build uses shared sources through Stonecutter, with 1.21.11 active and a
separate `forge` branch. These are targets, not a claim that all four artifacts
have passed testing. Use Java 21 or newer and the artifact matching both the
loader and exact Minecraft version. Runtime dependencies and installation
steps must be confirmed from the merged release metadata; this content set
introduces no third-party runtime dependency.

The intended environment is **client required, server optional**. A compatible
server component is needed for synchronized playback; installing the client
alone must not be described as making everyone on a vanilla server see the
animation. Multiplayer behavior and controls await the runtime test pass.

## Author and replace emotes

Read [the animation specification](docs/ANIMATION_FORMAT.md) for Blockbench
setup, axis conventions, interpolation, sounds, props, and examples.
**Arm X = −90° means straight forward**, in degrees, with position offsets
in pixels. The player rig has six sibling bones; `body` is not a root bone.

The loading contract targets resource packs at
`assets/<namespace>/emotes/<name>.animation.json` and loose files directly in
`config/quarkos_emotes/emotes/` inside your Minecraft instance. Replacements
keep the same filename, animation key, and canonical ID. Runtime loading and
override behavior still need verification.

Validate content locally using Python 3.10+ and its standard library:

```sh
python3 tools/validate_emotes.py
```

The validator prints a table and exits with status 1 on an error. It checks
JSON/schema, bone names, times, vectors, interpolation, metadata, and the
built-in English names. It cannot assess how a motion looks in Minecraft.
The names in `lang/en_us.emotes.json` are a **merge fragment** for the code
session, which must add them to the actual `en_us.json` language file.

## Screenshots and release checks

> Screenshot placeholder: front-view gesture lineup — capture pending.
>
> Screenshot placeholder: oak table flip and seated pose — capture pending.
>
> Screenshot placeholder: emote selection interface and dance sequence — capture pending.

No screenshots, client runs, Gradle builds, or network checks were performed
by this content run. The parent session owns the four-target builds, Java
tests, and xvfb client harness. See the
[content handoff log](docs/store/STATUS-codex.md) for integration checks.

[Changelog](CHANGELOG.md) · [Credits](CREDITS.md) · [License](LICENSE)
