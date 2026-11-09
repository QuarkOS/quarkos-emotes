# QuarkOS Emotes animation format

This is the content contract for QuarkOS Emotes 1.0.0 (`quarkos_emotes`).
Files use the numeric Bedrock/Blockbench animation JSON subset below, plus an
optional **document-level** `quarkos` object. Runtime integration is being built
in parallel; passing the content validator does not establish renderer, sound,
resource-reload, or multiplayer compatibility.

## Files and identifiers

- UTF-8 JSON, with `format_version` exactly `"1.8.0"`.
- One emote per `<name>.animation.json` file.
- One entry in `animations`, named `animation.<namespace>.<name>`, for example
  `animation.quarkos_emotes.wave`. The built-in namespace is `quarkos_emotes`.
- The canonical emote ID is `<namespace>:<name>`. If present, `quarkos.id` must
  equal that ID. Keep the filename, animation key, and ID stable when replacing
  an animation. Use lowercase names and underscores; `/` can separate ID paths.
- `animation_length` is a positive finite number of **seconds**. The release
  authoring profile limits lengths and key times to 3,600 seconds.
- `loop` is required and is exactly `true`, `false`, or
  `"hold_on_last_frame"`. Do not introduce a separate `looping` field.
- `bones` is a nonempty object. `sound_effects` is optional.
- JSON comments, trailing commas, duplicate object keys, NaN, infinity, Molang,
  expressions, animation controllers, timelines, particles, and scale tracks
  are outside this content profile. Export numeric baked keyframes instead.

The validator deliberately checks this portable subset even if a particular
runtime importer also accepts other Bedrock features. It does not check whether
Minecraft actually contains an item, texture, or sound ID.

## Player rig, axes, and units

Author against the standard **Steve** Bedrock player rig. These are the only
canonical bone names; case and underscores matter:

| Bone | Bedrock pivot (pixels) | Purpose |
| --- | --- | --- |
| `head` | `[0, 24, 0]` | Head |
| `body` | `[0, 24, 0]` | Torso |
| `right_arm` | `[-5, 22, 0]` | Player's right arm |
| `left_arm` | `[5, 22, 0]` | Player's left arm |
| `right_leg` | `[-1.9, 12, 0]` | Player's right leg |
| `left_leg` | `[1.9, 12, 0]` | Player's left leg |
| `prop` | `[0, 0, 0]` before attachment/position | Optional prop transform |

Rotation is an Euler `[x, y, z]` vector in **degrees**, relative to the neutral
player pose. Position is a `[x, y, z]` **offset in pixels**, not a replacement
pivot; 16 pixels equal one Minecraft block. Zero rotation leaves arms hanging
at the sides. Bedrock model Y points up, so a negative position Y lowers a bone.
Player right is negative model X and player left is positive model X. The
supplied table is authored in front of the player on negative model Z.

**Fixed shared convention: on `right_arm` and `left_arm`, X = −90° points the
arm straight forward.** X near −180° reaches overhead. Do not negate the
authored X angle to compensate for Minecraft's render coordinates. This is
the same pose that should appear in the Blockbench Bedrock player preview.
Do not assume that positive right-arm Z means outward motion. Check the rig
visually; use Y/Z sparingly and describe unusual intent in `quarkos.notes`.

The six player bones are independent siblings for playback. `body` is the
torso, not an extra root transform inherited by all other bones. To lower the
whole player, offset all six bones. `sit` uses `[0, -10, 0]` on each bone and
legs at X = −90°. To turn the whole pose, animate the siblings too and move
their shoulder/hip pivots around the body origin. `dance_elaborate` does this
with position channels and unwrapped Y values from 0° to 360°. Preserve the
turn rather than converting it to the shortest path between equal angles.

Steve arms have no elbow, wrist, or finger bones. Small position adjustments
approximate palm contact for thinking, facepalm, and clap. Point is a full-arm
gesture. Check intersections on both classic and slim skins; the authored
shoulder pivots above are the classic rig's values. Positions and rotations
are visual poses, not entity movement, collisions, or actual jumps.

## Channels and interpolation

Each bone may have `rotation`, `position`, or both. Each channel is either a
constant vector or a nonempty keyframe object:

```json
{
  "rotation": {
    "0.0": [0, 0, 0],
    "0.4": {
      "pre": [-90, 0, 0],
      "post": [-90, 0, 0],
      "lerp_mode": "catmullrom"
    },
    "0.8": [0, 0, 0]
  },
  "position": [0, 0, 0]
}
```

Time keys are nonnegative decimal strings, in strictly increasing numeric
order as written, with no duplicate numeric times (including `"0"` and
`"0.0"`). Use forms such as `"0.0"`, `"0.125"`, `"1.0"`; no exponent notation.
Every time must be within `[0, animation_length]`. Every vector contains
exactly three finite JSON numbers, not booleans or numeric strings.

A vector key is shorthand for equal incoming (`pre`) and outgoing (`post`)
values with linear interpolation. An object key requires at least one of
`pre` and `post`; an omitted side uses the supplied side. Blockbench commonly
exports `post` alone, which is preserved by the intake tool. `lerp_mode` is
optional and defaults to `linear`. Keep pre/post equal for
continuous movement. Different values create an intentional discontinuity
at that key. The outgoing key controls the segment toward the next key:

| `lerp_mode` | Behavior |
| --- | --- |
| `linear` | Interpolate outgoing value to next incoming value |
| `catmullrom` | Catmull–Rom curve using adjacent keys; can overshoot |
| `step` | Hold the outgoing value until the next timestamp |

Before the first key, hold its incoming value. At/after the last key, hold
its outgoing value. Omitted channels supply zero offsets/rotation; omitted
bones are not animated by the emote. Include keys at 0 and at the length to
make entry, exit, and loop intent explicit. Eight to thirty deliberate keys
per moving channel are usually enough for a short gesture; constant channels
are appropriate for parts that genuinely stay still.

The optional `quarkos.easing` is `linear` (default) or `ease_in_out` in this
profile. Easing remaps progress **inside each linear segment**, not the
animation clock or the sound timestamps. `ease_in_out` is quadratic:
`2t²` for `t < 0.5`, otherwise `1 − (−2t + 2)² / 2`. It does not remap
Catmull–Rom or step segments. Use `linear` for a constant-speed spin or a
mechanically timed prop. Runtime blending into/out of a pose is separate.

## Playback and loops

| `loop` | Intended playback |
| --- | --- |
| `false` | Play once, then release the pose |
| `true` | Repeat at `animation_length` |
| `"hold_on_last_frame"` | Play once, then retain the final pose until cancelled |

Match loop endpoints for every animated channel. A full turn may differ by
360° and still match visually; positions must still match. Loops can begin in
their sustained pose and rely on the runtime's entry blend: thinking starts
with the hand already near the chin. Do not add an entry/exit gesture on every
repeat unless it is part of the rhythm. Hold animations must end in a stable
pose; do not append a neutral recovery to sit or table flip.

## Metadata extension

`quarkos` is optional. Put it next to `format_version` and `animations`,
**not inside** `animations.animation.…`:

```json
{
  "id": "quarkos_emotes:wave",
  "name": "emote.quarkos_emotes.wave",
  "icon": "minecraft:sunflower",
  "category": "greeting",
  "cancel_on_move": true,
  "easing": "ease_in_out",
  "notes": "Right arm reaches up, then waves gently."
}
```

All fields are optional. `id` identifies the emote and must agree with the
animation key. `name` is a nonempty translation key. Supply its value in the
normal resource-pack `assets/<namespace>/lang/en_us.json`. In this content
branch, built-in names are supplied in **`en_us.emotes.json` for the code
session to merge**; Minecraft does not load that fragment as English on its
own. `icon` is a namespaced item ID. `category` is one of `greeting`,
`reaction`, `dance`, `pose`, or `fun`. `cancel_on_move` is a boolean request
for movement cancellation, subject to the runtime's player settings.
`notes` is author-facing text, not displayed dialogue or executable syntax.

With no extension, the canonical ID still follows from the animation key.
For predictable names, icons, and grouping, include explicit metadata rather
than depending on runtime presentation defaults. This specification does not
set unspecified UI defaults.

## Sounds

Use vanilla sound-event IDs directly in a Bedrock sound timeline:

```json
"sound_effects": {
  "0.3": {"effect": "minecraft:entity.player.attack.strong"},
  "0.9": {"effect": "minecraft:entity.player.attack.strong"}
}
```

This object goes **inside the animation**, alongside `bones`. Time keys follow
the same ordering/range rules as channels. Each event has exactly one `effect`.
The content profile uses one event per timestamp. Event defaults are volume
1 and pitch 1; global runtime sound settings can scale or suppress them.
No sound alias map, resource download, or custom audio file is required for
vanilla sound IDs. There is no dedicated vanilla clap sound: the supplied
clap uses a player attack slap approximation at each contact.

For a single cue with explicit volume/pitch, use this alternative **inside
the document-level `quarkos` object**:

```json
"sound": {
  "id": "minecraft:block.wood.hit",
  "volume": 0.6,
  "pitch": 1.0,
  "at": 2.25
}
```

`id` is required; `volume`, `pitch`, and `at` default to 1, 1, and 0.
Volume is finite and nonnegative, pitch finite and positive, and `at` is
within the animation. Both mechanisms add cues; do not specify the same cue
twice unless two sounds are intentional. On a loop, events repeat with the
cycle; a held final pose must not repeatedly fire its last cue. Prefer events
before the exact end time to avoid ambiguity at a loop boundary.

## Props

Props are optional metadata and do not place blocks or grant inventory items.
The simple form is `"prop": {"item": "minecraft:apple"}`. It describes a
temporary item held at the right hand; placement/rendering requires runtime
support and in-game review.

The cube form explicitly declares a transform bone and attachment:

```json
"prop": {
  "model": "cubes",
  "cubes": [
    {"from": [-10, 10, -6], "to": [10, 12, 6], "texture": "minecraft:block/oak_planks"},
    {"from": [-9, 0, -5], "to": [-7, 10, -3], "texture": "minecraft:block/oak_planks"}
  ],
  "bone": "prop",
  "attach": "world"
}
```

Coordinates are pixels relative to the prop origin. Each cube needs `from`,
`to`, and `texture`; each `from` component must be less than the corresponding
`to` component. `texture` is a namespaced texture path without `textures/` or
`.png`. No explicit UV atlas or external geometry file is used in this subset.
Use either the item form or the cube form, not a mixture.

The cube form's `bone` must be `prop`. Animate it in `bones.prop` using the
same rotation/position channel rules. `attach` is `right_arm`, `body`, or
`world`: respectively a hand/arm anchor, a torso anchor, or the player's
activation location independent of limb movement. A world prop is still a
temporary emote visual. The table uses a floor-level origin, negative-Z
position offsets, and `world` attachment so the table can leave the hands.
Its full file contains a top and all four legs. Verify prop anchoring and
texture mapping in the client before treating the effect as release-ready.

## Complete minimal file

This complete `nod.animation.json` is a short one-shot numeric example:

```json
{
  "format_version": "1.8.0",
  "animations": {
    "animation.quarkos_emotes.nod": {
      "animation_length": 0.8,
      "loop": false,
      "bones": {
        "head": {
          "rotation": {
            "0.0": [0, 0, 0],
            "0.2": [-4, 0, 0],
            "0.4": [16, 0, 0],
            "0.6": [-2, 0, 0],
            "0.8": [0, 0, 0]
          }
        }
      }
    }
  },
  "quarkos": {
    "id": "quarkos_emotes:nod",
    "name": "emote.quarkos_emotes.nod",
    "icon": "minecraft:feather",
    "category": "greeting",
    "cancel_on_move": true,
    "easing": "ease_in_out"
  }
}
```

## Authoring in Blockbench

1. Create a **Bedrock Entity** project in Blockbench and load or build a Steve
   player reference rig with the pivots above. Use an existing Bedrock player
   template if your Blockbench setup supplies one. Keep head, torso, and limbs
   as siblings; a preview-only parent may be used, but bake its motion into
   the canonical bones before export. Do not export an additional root bone.
2. Name the seven supported groups exactly. The six player bones are enough
   for gestures; add `prop` only for an animated prop. Set the neutral pose to
   the normal standing player, arms down, using pixel units.
3. In Animate mode, create `animation.quarkos_emotes.<name>`. Set the length
   and loop mode. Test an arm at X = −90° first: it must point straight forward.
   If it does not, fix the rig/project rather than reversing the authored keys.
4. Key anticipation, the main action, and follow-through. Preview from the
   front as well as the side. Check hand/face contact and avoid accidental
   self-intersections. Preview looping gestures over several cycles. For
   held gestures, inspect the final stable frame.
5. Use numeric position/rotation keys. Select linear, Catmull–Rom, or step
   interpolation. Bake constraints, IK, and expressions; remove unsupported
   tracks. Keep the spin angles unwrapped if you want a complete turn.
6. Export via **Animation > Export Animations** (wording can vary by
   Blockbench release). Export one animation into `<name>.animation.json`,
   using Bedrock animation format `1.8.0`. Inspect the exported JSON: retain
   standard `loop` and sort times numerically. Numeric `pre`/`post` keys may
   supply either side alone or both sides.
7. Restore/add the document-level `quarkos` block after export. Blockbench may
   omit unknown metadata on round trips. For cube props, preserve the cube
   definition as well as the exported prop animation. Add any sound cues.
8. Run `python3 tools/validate_emotes.py path/to/<name>.animation.json` and
   correct all errors. Then review the replacement in Minecraft; schema
   validation cannot prove the renderer matches the Blockbench preview.

## Drop-in replacements and resource packs

Built-ins live at `src/main/resources/assets/quarkos_emotes/emotes/`.
The planned loading/override contract is:

1. Load bundled resources and enabled resource packs from
   `assets/<namespace>/emotes/<name>.animation.json` in pack priority order.
2. The last definition of a **canonical emote ID** wins. A pack can replace
   `quarkos_emotes:wave` by retaining that ID/key; changing the ID adds a
   different emote. Keep the same asset path as well for a direct replacement.
3. Load loose files from the Minecraft instance's
   `config/quarkos_emotes/emotes/` after resource packs, so they override by ID.
   Put files directly in that folder; recursive config scanning is not part
   of this profile. Use explicit `quarkos.id` to retain the intended namespace.
4. Request a resource reload (normally F3+T) or restart the client after
   installing changes. Reload/override behavior requires runtime verification.

A resource pack also needs an appropriate `pack.mcmeta` for the target game
version. Minecraft 1.21.1 and 1.21.11 use different resource-pack metadata;
start from a pack known to load in the intended version. Language entries go
in `assets/<namespace>/lang/en_us.json`, not `en_us.emotes.json`.

## Replacing built-in emotes with Blockbench exports

Run the Python-only intake tool from the repository root. Preview is the
default; it creates no output, backup, temporary file, or Python bytecode:

```sh
python3 tools/import_blockato.py
python3 tools/import_blockato.py /home/box/blockato/emotes --dry-run
python3 tools/import_blockato.py /home/box/blockato/emotes --apply
python3 tools/validate_emotes.py
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest tools.test_import_blockato -v
```

The tool recursively reads `*.json` files with an `animations` object,
including exports in subfolders and files containing multiple animations.
Other JSON documents (geometry, clip reports) and non-JSON files such as
logs, `.bbmodel`, PNG, and GIF are ignored. An intake containing only work
in progress is successful and reports no exports. Malformed JSON is reported
as skipped. An absent source directory is an error.

Matching uses the animation name first, then filename, then enclosing folders
from nearest to farthest **within the intake directory**. Namespace segments
and the `animation.` prefix are removed; matching ignores case and separators.
For example, `animation.player.wave` maps to `wave`, while `tableflip`,
`table-flip`, and `table_flip` map to `table_flip`. `dance` is usable only
when the native catalog contains one `dance_*` variant. With both bundled
dances present, supply `dance_groove` or `dance_elaborate` in the name, filename,
or folder. Two valid exports targeting one ID are both skipped; remove the
duplicate rather than relying on traversal order. Multiple prop aliases
mapping to `prop` in one animation are also rejected rather than merged.

For each replacement, the tool:

1. Copies the Bedrock animation payload, retaining bones, numeric keyframes,
   pre/post shorthand, interpolation, length, standard `loop` (including
   `hold_on_last_frame`), and `sound_effects` exactly as data. It splits a
   multi-animation export into one native file per emote and renames the key
   to `animation.quarkos_emotes.<id>`. JSON whitespace may change.
2. Renames prop bone aliases `table`, `prop_table`, and `item` to `prop`.
   All other bone names must already use the supported rig names.
3. Carries over the destination file's complete document-level `quarkos`
   block, including name, icon, category, sound, easing, notes, and cube/item
   prop. This retains the table top and four legs. A delivered prop bone
   without a metadata prop model is kept and produces a warning.
4. Validates each candidate **in memory** using `validate_emotes.py` before
   writing. Unsupported tracks, Molang, invalid loop modes/times, or retained
   metadata sound times outside the delivered length cause a skip. It does
   not silently repair, reorder, or discard exported data to pass validation.
5. With `--apply`, backs up each replaced file byte-for-byte under
   `tools/replaced/<UTC timestamp>/<id>.animation.json`, then writes via an
   atomic file replacement. Backups are created only for actual replacements.
   It runs the validator over the output directory afterward. Preview also
   validates the existing output, without writing candidates.

The report includes ID, source file, length, loop, bone/keyframe counts, and
`replaced`, `new`, or `skipped` with the reason; preview rows say “would
replace/create.” Invalid input, duplicate targets, failed writes, or failed
validation return exit status 1. Other valid candidates can still import;
check the whole report. Unmatched or ambiguous names are informational skips.
For a rollback, copy the desired backup file back to the built-in directory
and rerun validation. Preview a curated intake before applying a batch.

Unmatched animations stay skipped unless `--new` is supplied:

```sh
python3 tools/import_blockato.py path/to/intake --new
python3 tools/import_blockato.py path/to/intake --new --apply
```

New IDs use the normalized final animation-name segment and get generated
metadata with `minecraft:paper`, category `fun`, movement cancellation, and
`emote.quarkos_emotes.<id>` as the name key. Ambiguous names remain skipped
even with `--new`. Review the generated presentation/prop metadata and add
the corresponding English translation to `en_us.emotes.json` (and merge it
into the runtime language file) before the default validator/release. The
importer's explicit-directory validation checks content without requiring
that language fragment. `--output-dir` selects a different native catalog
and destination; `--backup-dir` selects another backup parent, useful for
testing on a copy.

Metadata preservation does not retime an old sound cue, reposition the table
geometry to a delivered rig, or update author-facing notes to describe a new
motion. Review those after replacement, especially when length or loop mode
changes. Schema validation cannot establish classic/slim skin clearance,
prop anchoring, sound timing, or renderer compatibility; preview the final
native file and inspect it in game.

## Validation

From the repository root, run:

```sh
python3 tools/validate_emotes.py
python3 tools/validate_emotes.py path/to/emotes/ path/to/wave.animation.json
```

The default command checks every bundled JSON file, unique IDs, and the
English names fragment. Explicit paths validate those files/directories
without requiring the repository language fragment. The table reports ID,
length, loop, bone count, and keyframe count. A constant channel counts as
one key; a timestamp counts as one key regardless of equal pre/post values.
Sound events are not included in that keyframe count. Invalid input or an
empty/missing input directory produces diagnostics and exit status 1; all
valid input produces exit status 0. No network or third-party Python package
is required.
