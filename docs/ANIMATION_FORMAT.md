# QuarkOS Emotes animation format

The runtime reads numeric Blockbench Bedrock animations (`format_version`
`1.8.0`), without executable Molang. The five built-in animations by **Blockato**
are byte-identical animator exports: `wave` (2 s), `thinking` (3 s), `tableflip`
(2.4 s), `facepalm` (2.4 s), and `clap` (2.4 s). All five are one-shot
(`"loop": false`), with animation names `animation.emote.<name>`. Their keys
are on a 20 fps grid and predominantly use Catmull–Rom interpolation. Keep the
exported files unchanged; put mod presentation settings in sidecars.

## Files, IDs and overrides

Both resource-pack layouts work:

```text
assets/<namespace>/emotes/wave.animation.json
assets/<namespace>/emotes/legacy.json
assets/<namespace>/emotes/tableflip/
  tableflip.animation.json
  tableflip.quarkos.json
  table_prop.geo.json
  table_prop.png
```

The same flat/folder layouts work below `config/quarkos_emotes/emotes/`.
Folder animation files must end in `.animation.json`; flat files may also end
in `.json`. Geometry and metadata JSON files are excluded from animation
scanning. Resource reload (F3+T) reloads animations, geometry and textures.

The default ID is `<resource namespace>:<filename stem without .animation>`;
`emotes/tableflip/tableflip.animation.json` is `quarkos_emotes:tableflip`,
regardless of its animation key. Loose config files default to namespace
`custom`. An explicit metadata `id` remains supported, including
`quarkos_emotes:wave` for a local override. IDs must use Minecraft resource-ID
characters. Resource packs load in priority order; the last definition of an
ID wins. Config files load afterward and can replace those IDs. Use the same
resource path when replacing an animation and its companion assets in a pack.

Ship only runtime animation JSON, metadata sidecars, prop geometry JSON and
textures. Do not ship `.bbmodel`, preview GIF/PNG, reference rigs, clipcheck
reports or authoring tools in an emote folder. Every `.geo.json` in that
folder is treated as a prop.

## Flat player rig and additive transforms

The six vanilla player bones are **siblings**: `head`, `body`, `right_arm`,
`left_arm`, `right_leg`, `left_leg`. Leaning or moving `body` does not carry
head or arms. Animate each sibling when moving the entire player. Vanilla
player outer layers follow their corresponding parts.

| Bone | Bedrock pivot, Steve | Bedrock pivot, Alex (slim) |
| --- | --- | --- |
| head | `[0,24,0]` | same |
| body | `[0,24,0]` | same |
| right_arm | `[-5,22,0]` | `[-5,21.5,0]` |
| left_arm | `[5,22,0]` | `[5,21.5,0]` |
| right_leg | `[-1.9,12,0]` | same |
| left_leg | `[1.9,12,0]` | same |

Apply animation **on top of the model's own vanilla pose and pivots**.
Positions are additive pixel offsets, rotations are additive Euler degrees,
and scale multiplies the baseline scale. Rest is vanilla idle. Do not replace
arm pivots with Steve values when rendering a slim model. A Steve shoulder
is Java Y=2; a slim shoulder is Y=2.5.

Coordinates follow Blockbench's standard Bedrock convention: front is **−Z**,
player right is **+X in the Blockbench viewport**, and Y points up. Exported
geo X is mirrored relative to that viewport (the right-arm geo pivot is −5).
Sixteen pixels equal one block; feet are at Y=0 in geometry. Bedrock position
keys map into Java model pixels as `[x,-y,z]`. Absolute geo coordinates map
as `[x,-y,z]` relative to the feet. Add 24 Java model pixels to reach the
player model's origin above the feet. Java Euler angles are `[x,y,z]` in
radians, applied in Z/Y/X order. This follows the animator's measured
Blockbench transform: viewport Euler `[-x,-y,+z]`, then reverse viewport X
and Y to enter Java model space.

Arm X=−90° reaches forward. Blockato's right-arm wave Z=+148° raises the arm
outward, and position X=−1.5 moves the right shoulder outward by 1.5 pixels.
These examples distinguish the X/Z sign convention from older generic
conversion recipes. Position and rotation never move the actual entity or
change its collision box.

## Channels, interpolation and playback

A bone may contain `rotation`, `position` and `scale` channels. A channel is
a constant numeric vector/scalar or an object of timestamped keys:

```json
"rotation": {
  "0": {"post": [0,0,0], "lerp_mode": "catmullrom"},
  "0.5": {"pre": [-90,0,0], "post": [-90,0,0], "lerp_mode": "catmullrom"},
  "1": [0,0,0]
}
```

Times are seconds. Length is a positive number up to 3,600 seconds. Vector
components are finite numbers; author baked numeric keys instead of Molang.
`pre` is the incoming value and `post` the outgoing value. If only one is
present, it supplies both. Before/after the timeline, hold the first incoming
or last outgoing value. Omitted rotation/position channels supply zero;
omitted scale supplies one. Omitted bones retain the vanilla pose.

The outgoing key's `lerp_mode` controls the next segment: `linear` interpolates
values, `step` holds the outgoing value, and `catmullrom` uses neighboring
keys and can overshoot. Optional metadata `easing` remaps only linear
segments; keep `linear` for faithful exported curves. Do not wrap rotation
angles to the shortest path: authored full turns must remain full turns.

`loop: false` plays once, `true` repeats, and `"hold_on_last_frame"` holds
until cancelled. Entry and exit blend for 0.15 seconds. Props exist only
while playback is active and hide as soon as completion or cancellation starts;
the player pose alone blends out. Prop transforms follow the full authored
trajectory, so blending an inverted table back to rest cannot pull it through
the floor. Movement cancellation follows the emote metadata
and global setting.

## Props: geometry + matching PNG

Load **every** `<base>.geo.json` next to a folder animation together with
`<base>.png`. Geometry uses Bedrock `format_version: "1.12.0"` and
`minecraft:geometry`, a description with texture dimensions, and named bones
containing cubes. Box UVs (`"uv": [u,v]`), cube `origin`/`size`, bone and cube
`pivot`/`rotation`, mirror and inflate are supported. Texture dimensions must
match the PNG. Bone parents, when supplied, refer to other geometry bones.
Per-face UV objects and material/controller systems are outside this subset.

A prop binds to animation channels by the **exact bone name inside the geo**.
The shipped `table_prop.geo.json` defines `table`; animation channels are
`bones.table`. Never rename that bone to `prop` or bind using `table_prop`.
Use unique prop names that do not clash with the six vanilla bones.

Props attach to the **player root**, rotate with body yaw, and inherit no
limb transform. They are temporary visuals, not placed blocks or inventory
items. Render with the matching PNG using `entityCutout`, entity light and
hurt overlay. Geometry pivots are absolute Bedrock pixels: apply the animated
bone transform about that pivot, then each cube's local pivot/rotation.
The table's pivot is `[0,0,-20]`; its legs start at Y=0 and its entire rest
geometry is in front at negative Z. Its exported animation flips around that
pivot while keeping the table above the floor. Local config PNGs are loaded
as dynamic textures and released/reloaded on resource reload.

Legacy inline `quarkos.prop` item attachments remain available, but new
Blockbench props should use the folder geometry/PNG contract above.

## Sidecar metadata and sound

For an export without `quarkos`, read `<animation stem>.quarkos.json` next to
it. The sidecar contains the metadata object directly:

```json
{
  "id": "quarkos_emotes:wave",
  "name": "emote.quarkos_emotes.wave",
  "icon": "minecraft:sunflower",
  "category": "greeting",
  "cancel_on_move": true,
  "easing": "linear"
}
```

The display name may be a translation key or literal text; translation keys
go in `assets/<namespace>/lang/en_us.json`. Defaults are the title-cased ID
(e.g. `My Dance`), `minecraft:note_block`, category `general`, movement
cancellation enabled, and linear easing. Existing animation-level `quarkos`
blocks take precedence over document-level blocks; document-level blocks
take precedence over the sidecar. Metadata is selected as a complete object.

Optional `sound` metadata accepts `id`, `at` (seconds, default 0), `volume`
(default 1) and `pitch` (default 1). Standard animation `sound_effects` timelines
also work, with `{ "effect": "minecraft:block.wood.hit" }` or an array of
events at each timestamp. Both mechanisms add cues. The shipped clap sidecar
adds a vanilla attack sound at 0.6 s; the table adds wood impact at 1.1 s.
Global sound settings may mute/scale these cues. No custom audio is bundled.

## Import and validate

```sh
python3 tools/import_blockato.py path/to/curated-intake --dry-run
python3 tools/import_blockato.py path/to/curated-intake --apply
python3 tools/import_blockato.py path/to/curated-intake --new --apply
python3 tools/validate_emotes.py
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v
```

The importer matches animation name, filename, then enclosing folders against
the catalog (ignoring case/separators); ambiguous names and duplicate targets
are skipped. `tableflip` is the built-in ID. Unmatched names require `--new`.
Use a curated intake containing only the desired replacements: the animator's
work directory may contain additional animations outside this built-in set.

It preserves animation and bone names, numeric channels, loop modes and sounds.
Single-animation files copy byte-for-byte; multi-animation documents split
without renaming keys. Runtime folder assets (`*.geo.json` and their matching `*.png`) copy
together, excluding authoring projects and reports. A delivered sidecar wins;
otherwise existing destination metadata is kept in a sidecar, or defaults
are generated. Metadata is never injected into an animator export. Replaced
animation, sidecar, geometry and PNG files are backed up under
`tools/replaced/<UTC timestamp>/` with relative paths preserved. Flat
replacements migrate to the folder layout. Preview writes nothing.

The validator checks numeric animation content, unique IDs, companion geometry,
box UVs, matching PNG dimensions, sidecars, and built-in English translations.
It ignores geometry/sidecar JSON as animation candidates. Explicit paths
validate without requiring built-in translations. Invalid content returns 1.
Neither validation nor Java tests establishes in-game appearance: review both
Steve and Alex, prop texture orientation, end/cancel/reset, first-person view,
armor, held items and each loader/version before release.
