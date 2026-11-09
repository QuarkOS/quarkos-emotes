# QuarkOS Emotes

Editable player emotes for Minecraft by **Emilio Schwaiger / QuarkOS**,
with five built-in animations by Blockbench animator **Blockato**.

The set contains **16 emotes**: `wave`, `thinking`, `tableflip`, `facepalm`,
`clap`, `bow`, `cheer`, `dab`, `dance_elaborate` (Spotlight Dance),
`dance_groove` (Groove), `laugh`, `point`, `salute`, `shrug`, `sit`, `stretch`.

Blockato's wave, thinking, tableflip, facepalm and clap play once. Tableflip
includes a textured temporary table bound to the animation's `table` bone.
Cheer, laugh and the two dances loop; sit holds its final pose until cancelled.
The five Blockato exports and the table geometry/texture are unchanged from
the animator's files. See [credits](CREDITS.md).

## Targets and status

Minecraft **1.21.1 and 1.21.11**, on **Fabric and Forge**, using Java **21+**.
Use the artifact matching both your loader and exact game version.
The parent verified the four Gradle builds and original 14 Java tests before
this animator integration. New contract changes require another Gradle pass;
visual and multiplayer verification remain release checks. This repository
is release preparation, not a claim of published downloads.

Client installation provides local playback. A compatible server component
and shared animation set are needed for synchronized playback. Installing the
client alone does not make other players on vanilla servers see the animation.

The wheel supports paging, search and favorites; settings include sounds,
movement cancellation and first-person behavior. Review current loader metadata
for dependencies. No third-party animation library is introduced by this set.

## Author or replace emotes

Read [the animation contract](docs/ANIMATION_FORMAT.md). The six player bones
are siblings; transforms add to vanilla poses and preserve Steve/Alex pivots.
Files use numeric Bedrock animation JSON. Flat `emotes/*.json` or
`*.animation.json` and folder `emotes/<name>/*.animation.json` layouts work
in resource packs and `config/quarkos_emotes/emotes/`. Put metadata in
`<name>.quarkos.json` to keep Blockbench exports untouched. Props load every
same-folder `.geo.json` plus its matching PNG, bind by geo bone name, and
attach to the player root for the duration of the emote.

```sh
python3 tools/validate_emotes.py
python3 tools/import_blockato.py path/to/curated-intake --dry-run
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v
```

The importer supports preview, backups, folder assets and metadata preservation.
English emote names are in the runtime `lang/en_us.json`.

## Release checks

The opt-in xvfb harness captures `emote-wave`, `emote-thinking`,
`emote-tableflip` (front/back/side), `emote-facepalm`, `emote-clap`, the other
emotes, the wheel/config UI and first-person wave. Screenshots and results
from the parent's separate worktree are pending integration. Inspect both
classic and slim skins, table UVs and floor placement, and end/cancel reset.
Dedicated-server startup and two-client synchronization also need verification.

[Changelog](CHANGELOG.md) · [License](LICENSE) · [Handoff log](STATUS-codex.md)
