# QuarkOS Emotes animation format

The runtime reads numeric Blockbench Bedrock animations (`format_version`
`1.8.0`), without executable Molang. There are exactly **42 built-ins**:
41 emotes by **Blockato** (final batches 1–4 with v5 revisions), plus stretch.
Two companion chain clips add files, never IDs. The delivery contract is
`NOTES_v5.md`, `RENAMES_v2.md`, `alex_recheck_v5.txt` and `md5_delivered.txt`.
All 47 animator runtime files match the current canonical delivery, including
Floss v2 and Sword Salute v2. The categorized list is in [README](../README.md).
Eight main clips loop, five hold, and 29 play once. Keep animator exports
byte-identical and presentation settings in sidecars.

Sit has ID `quarkos_emotes:sit`, key `animation.emote.sit`, and a 1.10-second
hold clip. Load **only `sit/sit.animation.json`** (now byte-identical to the
old sit_v2). Ignore sit_v1/sit_v2 and `_archive`; no mode-dependent filename
fallback remains. `sit_full/sit_full.animation.json` retains the four-second
one-shot and delivered `animation.emote.sit_full` key.

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
In a folder `<folder>/`, read only `<folder>.animation.json`, optional
`<folder>_enter.animation.json` and `<folder>_loop.animation.json`, prop pairs
`<base>.geo.json` + `<base>.png`, and `<folder>.quarkos.json`. Each prop needs
both files. Ignore every other file (sit_v1/sit_v2, clipcheck, GIF, bbmodel,
preview PNG, extra JSON). Skip any directory whose name begins with `_`,
including its descendants. This rule is identical for built-ins, packs, config,
the importer and directory validation. Flat legacy `emotes/*.animation.json`
and plain `.json` animations continue to work; they have no chain companions. Resource reload (F3+T) reloads animations, geometry and textures.

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
reports or authoring tools in an emote folder. Every matching geometry/PNG pair in that folder is treated as a prop.

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
and scale multiplies the baseline scale. Rest is vanilla idle. By default
(`"arm_pivot": "model"`) retain the model's own pivots: a Steve shoulder is
Java Y=2; a slim shoulder is Y=2.5. For an emote with `"arm_pivot": "steve"`,
shift slim arm pivots by **−0.5 Java pixels** at full weight (Bedrock Y=22).
Blend the shift with the entry/exit weight; model defaults reset each frame,
so cancellation, completion and the next emote restore Y=2.5. Wide models and
unflagged emotes retain their own pivot. First-person arms use the same rule.

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

## Whole-body orbit without a root bone

Backflip, spin, worm, sleep, lay_down and pushups are baked across all six sibling bones. The
animator writes each bone's composed **unwrapped Euler rotation** and an
orbit position offset `G(pivot) − pivot` around a shared centre. Apply those
channels directly; adding a root transform would apply the movement twice.
Orbit offsets use Steve's pivots, so these six sidecars set `"arm_pivot": "steve"`.
The same policy applies to pushups_enter through its main emote. Sword Salute
also opts in so its root sword and the Alex arm share pivot Y=22. All other
emotes retain model pivots, following the v5 Alex recheck (sword is the explicit
exception requested after that report).
Otherwise Alex's 0.5-pixel pivot difference yields shoulder drift of
`|(R−I)·(0,0.5,0)|` (0.71 px at 90°, 1 px at 180°). The lying sleep/worm head
also has an intentional 2-pixel offset to rest at torso height; retain it.

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

At a key's exact time, use **post** (`t >= key time`), including render partial
ticks. Before it, interpolate only toward **pre**. Differing pre/post values
make an instant jump; do not ramp across it. Distinct timestamp spellings that
parse to the same number (e.g. `"0.25"` and `"0.250"`) merge stably: first pre,
last post and outgoing mode. Exact duplicate JSON property names are rejected
by the offline validator; use pre/post instead. Catmull–Rom tangents stop at a
discontinuity so the other side cannot pull a curve across the jump. Step holds
the old outgoing value until the next key, then takes that key's post immediately.
Playback counts 20 Hz ticks from an anchor instead of accumulating 0.05 seconds,
so the shipped sword switches on tick 5 and off tick 44 without a late frame.

Scale accepts a uniform number or `[x,y,z]`, as a constant, timed value, or
pre/post value. Omitted scale is `[1,1,1]`. For props, if **any axis has absolute
scale < 1e-4**, skip the whole bone's cubes before matrix/quad submission; its
prop children also hide. Negative nonzero scales mirror. Scale-zero props do
not emit degenerate quads. This prop rule never changes player limb visibility;
player scale changes only when that player's scale channel is keyed, and an
explicitly scaled-to-zero player parent also hides its attached props.

The outgoing key's `lerp_mode` controls the next segment: `linear` interpolates
values, `step` holds the outgoing value, and `catmullrom` uses neighboring
keys and can overshoot. Optional metadata `easing` remaps only linear
segments; keep `linear` for faithful exported curves. For `loop: true`,
Catmull–Rom wraps neighbours as Blockbench 5.2.2 does: before the first key
use the second-to-last key, and after the last key use the second key. Author
equal first/last seam keys; uniform seam intervals give continuous velocity.
Non-looping clips retain clamped neighbours.

Rotation values are **raw, unwrapped Bedrock Euler degrees**, interpolated
keyframe by keyframe **per component** for Catmull–Rom, linear and step.
Never normalize angles modulo 360°, choose a shortest path, or quaternion-slerp
between keys or during entry/exit blends. The parser keeps signed values;
`Track` samples components, `BedrockAxes.rotation` converts degrees to radians,
`PoseMath` blends each component, and `EmotePose` writes ModelPart's Euler fields.
Backflip body X passes through −180° at 0.8 s and reaches −360°; spin body Y
passes through 270.5° at 0.9 s and ends at +360°. A matrix/quaternion used to
render the resulting Euler pose is fine; quaternion interpolation is not.

`loop: false` plays once, `true` repeats, and `"hold_on_last_frame"` holds
the final frame indefinitely after playing once, until movement/cancellation
(subject to movement settings). Entry blends for 0.15 seconds; cancellation
and natural one-shot completion blend the final/current pose back to the
default pose over 0.2 seconds. A cancelled pose freezes throughout that fade.
Holding is derived locally from play time: only play and stop packets are
needed. Movement, jump, attack, hurt, death and explicit cancel use the same
stop path, broadcasting stop so remote viewers also blend out. Props exist only
while playback is active and hide as soon as completion or cancellation starts;
the player pose alone blends out. Prop transforms follow the full authored
trajectory, so blending an inverted table back to rest cannot pull it through
the floor. Movement cancellation follows the emote metadata
and global setting.

## Chaining and elapsed time

A folder's optional `<name>_enter.animation.json` must be a one-shot. It plays
once, then the main `<name>.animation.json` starts at t=0. Pushups enters the
plank for 1.3 seconds, then repeats its 2-second main loop without reentering.
An optional `<name>_loop.animation.json` must loop and is used only when the
main clip is `hold_on_last_frame`. After the main reaches its last frame it
switches directly to that loop; meditate enters over 5.6 seconds, then repeats
a 2-second breath until movement/cancel. Without companions ordinary playback
is unchanged: once, loop or held last frame. Invalid companion files are logged;
the main emote remains available without the invalid chain.

Handoff frames must match; there is **no blend between segments**. Blend-in
happens only at the start of the emote. Cancellation in any segment freezes
that segment and phase and fades the player pose over 0.2 seconds. Props hide
immediately. Natural completion of entry + one-shot main uses the same fade.
Metadata, pivot policy, props and the public ID always belong to the main emote;
companions supply their own animation tracks, interpolation and sound timeline.

The wire still sends one emote ID and total elapsed milliseconds. For elapsed
`E` and entry length `A` (zero if absent): while `E < A`, sample entry at `E`;
otherwise let `T = E - A`. A repeating main samples `T % main_length`; a once
main clamps at its end. For a held main with a loop companion of length `L`,
when `T >= main_length` sample `(T - main_length) % L` in the loop companion.
Otherwise hold the main endpoint. Late viewers and resource rebinds use these
same equations, skip historical sounds and keep advancing normally.

`Emote.length()` is the main clip length. `totalLength()` is entry + main for
one-shots and infinity for repeating or held emotes; the 0.2-second fade is
excluded. `EmotePlayer.time()` reports the current segment phase; `segment()`
identifies enter/main/loop. Neither companion creates a wheel or network ID.

## Props: geometry + matching PNG

Load **every** `<base>.geo.json` next to a folder animation together with
`<base>.png`. Geometry uses Bedrock `format_version: "1.12.0"` and
`minecraft:geometry`, a description with texture dimensions, and named bones
containing cubes. Box UVs (`"uv": [u,v]`), cube `origin`/`size`, bone and cube
`pivot`/`rotation`, mirror and inflate are supported. Texture dimensions must
match the PNG. Bone parents may refer to another geometry bone or one of the
six player bones; unknown parents and parent cycles are rejected.
Per-face UV objects and material/controller systems are outside this subset.

A prop binds to animation channels by the **exact bone name inside the geo**.
The shipped `table_prop.geo.json` defines `table`; animation channels are
`bones.table`. Never rename that bone to `prop` or bind using `table_prop`.
Use unique prop names that do not clash with the six vanilla bones.

Props without a parent attach to the **player root** and rotate with body
yaw (tableflip). A geometry bone with `"parent": "right_arm"`, for example,
inherits that flat player's animated pivot + position + Z/Y/X rotation + scale,
then applies its own rest rotation and animated channels about its own pivot.
Cube transforms follow last. In matrix order this is
`M_player_parent * M_prop_bone * M_cube`. Geometry pivots remain absolute
Bedrock coordinates; subtract the parent's rest pivot before applying its
posed ModelPart transform. The final rendered limb includes blend weight,
vanilla baseline and the Steve/Alex pivot policy. A rotated body cannot carry
a right-arm child because the six player bones stay siblings.

The delivered sword uses the flat convention: no parent and copied
right_arm rotation/position tracks. Preserve that export as delivered; adding
a parent would apply its arm motion twice. A synthetic parented prop is covered
by tests. Its geo bone is `sword`, with no parent and pivot `[-5,22,0]`, exactly
the Steve right-arm pivot. The shipped scale track pops from 0 to 1 at 0.25 s
and from 1 to 0 at 2.20 s using pre/post keys. The animator's
`sword_salute_v2_swordcheck.json` measured Steve drift 0 and Alex drift 0.7918 px
with the old 21.5 pivot. Our `arm_pivot: "steve"` removes that rotating pivot
mismatch for Alex; slim cube dimensions remain slim. The 32×32 sword texture
is the animator's provisional flat-colour artwork and needs visual sign-off.
Props are temporary visuals, not placed blocks or inventory
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

Optional `arm_pivot` accepts `"model"` (default) or `"steve"`; invalid values
are rejected. It controls the temporary slim arm-pivot override described above.
Representative capture times live in the harness table, preserving animator bytes.

Optional `sound` metadata accepts `id`, `at` (seconds, default 0), `volume`
(default 1) and `pitch` (default 1). Standard animation `sound_effects` timelines
also work, with `{ "effect": "minecraft:block.wood.hit" }` or an array of
events at each timestamp. Both mechanisms add cues. The shipped clap sidecar
adds a vanilla attack sound at 0.6 s; the table adds wood impact at 1.1 s.
Global sound settings may mute/scale these cues. No custom audio is bundled.

## Import and validate

```sh
python3 tools/import_blockato.py --dry-run
python3 tools/import_blockato.py --apply
# Optional: limit an import to a curated intake
python3 tools/import_blockato.py path/to/curated-intake --apply
python3 tools/validate_emotes.py
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v
```

The importer matches animation name, filename, then enclosing folders against
the catalog (ignoring case/separators); ambiguous names and duplicate targets
are skipped. `--apply` adds new names and updates changed folders from
`/home/box/blockato/emotes/` by default; `--new` remains a compatibility flag.
Every apply recopies the selected runtime files even when unchanged; only
changed content needs a backup. Each animation, chain and prop file reports
an MD5 match against `md5_delivered.txt` (when present) and installed bytes;
a mismatch makes the command fail. Dry runs report source/ledger hashes only.
New sidecars use a readable literal name, paper icon, fun category and movement
cancellation; review presentation before release. Existing sidecars are kept
(including formatting), unless the source delivers replacement metadata.
Omit `--include` to refresh the complete final set. The importer excludes
`_archive/`, `rig/` and `tools/`; strict filename discovery handles all emote
folders uniformly. Conflicting duplicate targets remain errors.

It preserves animation and bone names, numeric channels, loop modes and sounds.
Single-animation files copy byte-for-byte; multi-animation documents split
without renaming keys. Runtime folder assets (`*.geo.json` and their matching `*.png`) copy
together, excluding authoring projects and reports. A delivered sidecar wins;
otherwise existing destination metadata is kept in a sidecar, or defaults
are generated. Metadata is never injected into an animator export. Replaced
animation, companion chain, sidecar, geometry and PNG files are backed up under
`tools/replaced/<UTC timestamp>/` with relative paths preserved. Flat
replacements migrate to the folder layout. Removed prop pairs and companion clips are backed up
and removed from the destination. Preview writes nothing.

The validator checks numeric animation content, unique IDs, companion geometry,
box UVs, matching PNG dimensions, sidecars, and built-in English translations.
It ignores geometry/sidecar JSON as animation candidates. Explicit paths
validate without requiring built-in translations. Invalid content returns 1.
Neither validation nor Java tests establishes in-game appearance: review both
Steve and Alex, prop texture orientation, end/cancel/reset, first-person view,
armor, held items and each loader/version before release.
