# QuarkOS Emotes

Say hello with a wave, react with a facepalm, or celebrate with a dance.
QuarkOS Emotes is an editable player-animation project for Minecraft by
**Emilio Schwaiger / QuarkOS**.

**1.0.0 preparation status:** the sixteen initial animation files and content
documentation are authored. The runtime, four target builds, and in-game
screenshots still require verification. This description is draft release
copy; it does not claim that tested downloads are available.

## Initial emote set

Wave, thinking, facepalm, table flip, clap, bow, salute, laugh, dab, sit,
groove, spotlight dance, shrug, point, cheer, and stretch.

The two dances include a short rhythmic groove and a longer routine with
arm waves, steps, a full turn, and hip sway. Thinking, clap, laugh, dance,
and cheer are authored as loops. Sit and table flip keep their final pose.
Clap has two vanilla slap-like sound cues per cycle; the table prop uses
five oak-plank cubes. Rendering and sound playback remain under review.

These initial animations use the classic Steve rig, without fingers or
elbows. Close hand contact and slim-model proportions still need a visual
pass. Higher-quality Blockbench replacements may be incorporated before
release while keeping the same emote IDs.

## Editable animation content

The native files are numeric Bedrock/Blockbench `.animation.json` documents.
The repository documents the bone names, pixel units, degree rotations,
loop modes, interpolation, metadata, sound cues, and temporary props.
An offline Python validator checks the supported content schema.

Resource-pack replacements in `assets/<namespace>/emotes/` and loose files
in `config/quarkos_emotes/emotes/` are the runtime loading targets. Same-ID
replacements, resource reloads, and custom-file behavior need verification
in the merged runtime before publication.

## Compatibility targets

- Minecraft **1.21.1** and **1.21.11**.
- **Fabric** and **Forge**, using the artifact matching the exact game version.
- Java **21 or newer**.
- Environment: **client required; server optional**.

Optional server-assisted synchronization is being implemented and tested.
Installing only a client mod does not guarantee other players can see the
animation on a vanilla server. Final dependency requirements and multiplayer
instructions will accompany verified release artifacts.

## Images to add after client verification

> Screenshot placeholder: third-person front view of the gesture set.
>
> Screenshot placeholder: table flip, sit, and dance sequence.
>
> Screenshot placeholder: emote selection interface.

By **Emilio Schwaiger / QuarkOS**. Open source under **MIT**. Referenced vanilla
Minecraft textures and sounds are supplied by Minecraft, not redistributed
as part of this content set.
