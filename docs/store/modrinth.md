# QuarkOS Emotes

Wave hello, bow, celebrate, or let a facepalm do the talking. QuarkOS Emotes
is a Minecraft player-animation project by **Emilio Schwaiger / QuarkOS**,
with an open, editable Bedrock/Blockbench animation format.

**Release preparation status:** version 1.0.0's initial content contains
sixteen authored emotes. Runtime integration and testing are in progress;
the target loaders and features below are not yet a verified compatibility
claim. This page is draft store copy, not an announcement of a published build.

## Sixteen ways to react

- **Greetings:** wave, clap, bow, salute, and point.
- **Reactions:** thinking, facepalm, laugh, shrug, and cheer.
- **Dance:** a two-second groove and an eight-second spotlight routine with
  arm waves, steps, a full turn, and hip sway.
- **Poses and fun:** sit, stretch, dab, and an oak table flip.

Thinking, clapping, laughing, dancing, and cheering repeat. Sit and table
flip are authored to keep their final pose until cancelled. Short gestures
include a lead-in and a recovery. The table is built from five textured
cubes, and clap uses timed vanilla slap-like sounds. These effects still
need renderer and sound verification in the client.

The initial set uses the classic Steve rig. There are no finger or elbow
bones: pointing uses the whole arm and close hand poses use small position
offsets. Classic and slim player proportions need an in-game review before
the final screenshots and release files are selected.

## Make the gestures your own

The documented authoring workflow uses Blockbench's Bedrock entity/player
rig and **Animation > Export Animations**. Animation files contain readable
numeric rotation and position keys, standard loop modes, and an optional
`quarkos` metadata block for names, icons, categories, sounds, and props.

The runtime integration targets resource-pack replacements under
`assets/<namespace>/emotes/` and a local
`config/quarkos_emotes/emotes/` folder. Keep an emote's ID to replace it;
choose a new ID to add a distinct animation. Loading, reload, and override
behavior remain part of release verification.

The repository includes a Python validator and a full animation-format
guide. The authored animation files are MIT licensed, so they can be studied
and adapted under that license.

## Target versions and multiplayer

Version 1.0.0 targets **Minecraft 1.21.1 and 1.21.11**, on **Fabric and Forge**.
Use an artifact built for your exact version and loader, with Java 21 or
newer. Required runtime dependencies will be specified by the final release
metadata after the build matrix passes.

The intended environment is **client required, server optional**. Local
playback and optional server-assisted synchronization are runtime goals
under test. Do not expect players on a vanilla server to see your animations
simply because you installed the client. A compatible shared animation set
and verified synchronization support are needed for shared playback.

## Screenshots

> Screenshot placeholder: front-view wave, salute, and facepalm lineup.
>
> Screenshot placeholder: seated player and oak table flip.
>
> Screenshot placeholder: dance sequence and emote selection interface.

Screenshots will be added after the client harness runs. No rendered images
are represented by these placeholders.

**Author:** Emilio Schwaiger / QuarkOS · **License:** MIT · **Version:** 1.0.0
