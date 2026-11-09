# QuarkOS Emotes design

The shared source tree is processed by Stonecutter for Fabric and Forge on
Minecraft 1.21.1 and 1.21.11. Loader bootstraps register keys, resource reloads,
and networking; client tick/model mixins implement the same playback behavior.
Common/server bootstraps do not initialize Minecraft client classes.

## Own animation layer

An immutable Bedrock animation model, Gson importer, keyframe interpolation,
axis conversion, override registry, and tick-based EmotePlayer have no Minecraft
imports. We own this small layer to avoid a playerAnimator runtime dependency,
keep both loaders on the same behavior, and make animation math testable without
a game launch. Minecraft supplies Gson; Mod Menu is compile-only and optional.

The parser reads multiple animations per file, pre/post discontinuities, linear,
step and Catmull–Rom keys, easing, sounds, and the optional quarkos extension.
Unsupported Molang becomes zero with a warning, never executable code. Malformed
animations return errors while other animations in the file can still load.
Resource definitions are applied in pack order and then filename order; the last
id wins. Sorted config/quarkos_emotes/emotes/*.json files load last as namespace
custom. F3+T reloads both resource packs and local drop-ins.

## Coordinates and rendering

The fixed authored player-bone convention is **X = -90 degrees points an arm
straight forward**. In Java's down-Y ModelPart coordinates this is xRot=-pi/2;
negating it a second time points the arm backwards. Y/Z rotation signs flip;
positions map (-X,-Y,+Z) in model pixels. No 1/16 conversion is applied to a
ModelPart translation; PoseStack prop translations divide pixels by 16.
The axis tests check the resulting arm direction at both -90 and +90 degrees.
This explicit convention takes precedence over generic Bedrock coordinate
recipes that assume a different bind-space orientation.

PlayerModel setupAnim TAIL applies authored transforms after vanilla animation,
with a 0.15 second blend envelope. On 1.21.1 the rig resets before vanilla setup
and copies transforms to the separate outer layers afterward. On 1.21.11 the
outer layers are child parts and inherit transforms. First-person hand hooks
apply the same arm samples after vanilla's hand reset. The default first-person
mode temporarily switches to third-person back and restores the camera when
playback finishes, including world disconnects.

Props use a render layer attached to the selected arm/body bone or an independent
animated prop bone in feet-origin space. Items use Minecraft's own item renderer.
Cube lists and Bedrock geometry cube origins/sizes/pivots/rotations render using
block-item geometry (oak planks by default, or a per-cube item). Texture UVs,
geometry bone hierarchy and mesh-specific materials are not implemented yet.

## Networking and fallback

Fabric uses CustomPacketPayload and PayloadTypeRegistry; Forge uses an optional
ChannelBuilder SimpleChannel. C2S contains only an id (empty means stop). The
server supplies the sender UUID, checks the 128-character id syntax, rate-limits
requests, and forwards S2C to viewers with the channel. Server animation assets
are unnecessary: clients resolve ids from their own resource packs. Unknown ids
are ignored. The initiating client plays immediately and ignores its server echo.

Both loaders check remote channel presence before sending. Without the server
mod, playback stays local and a one-time chat hint explains visibility. Fabric's
mod environment is '*' and Forge's displayTest is IGNORE_ALL_VERSION; vanilla
peers remain compatible. Late trackers currently receive the next play/stop
request rather than a snapshot of an already-running emote.

## UI, config, and validation

The eight-sector wheel has paging, search, number selection, hold/release or click
mode, and up to eight favorites. Right-click/F edits favorites; four unbound keys
play the first four. The config screen persists atomically to
config/quarkos_emotes.json and previews the selected animation without sending a
network play request. Preview requires an in-world player.

The opt-in client harness creates a creative flat world, fixes time/weather,
freezes each selected emote at a representative authored key (wave .5 s,
clap contact .6 s, thinking 1.2 s, facepalm .65 s, table in flight .75 s).
The remaining per-emote times live in ClientHarness.poseTime. Captures include
front/¾ views of all sixteen emotes and back/side views of tableflip and dances.
A harness-only Camera mixin moves the camera closer, lowers its target, and
orbits it without twisting the player's head. Table views include a forward
framing offset; sit has a lower target so its feet stay visible.

First-person SHOW_ARMS uses a stable player-space transform for both arms instead
of vanilla's rotated item-holding transform, temporarily replacing held items.
The first-person harness shot keeps the HUD enabled because F1 also disables
Minecraft's hand renderer. It checks the hand pose hook independently. The
camera-mode shot starts in first person and calls the real play path to check
that THIRD_PERSON_CAMERA switches and renders the emote. Screenshots are saved
at frame end, after deferred rendering. Full runs have 44 captures / 18 checks.

Use -Pharness=true -Pshots=<directory>, optionally -Ponly=wave,ui-wheel and
-Pwidth/-Pheight. -PguiScale=<n> reaches both loaders as the system property
quarkos_emotes.guiScale (0 = auto). ui-wheel-s<n>.png names the requested scale;
EMOTE_SHOT logs the effective scale as well. Minecraft clamps scales to preserve
its minimum GUI size, so 854x480 requests 2–4 normally all use effective scale 2.
The wheel and config layout fit the minimum 320x240 logical GUI. Full Gradle
builds and xvfb captures are parent-run checks; this implementation session
uses cached jars and javac only.
