#!/usr/bin/env python3
"""Validate the Quark's Emotes Bedrock subset using only Python's standard library."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
BUILTINS = ROOT / "src/main/resources/assets/quarkos_emotes/emotes"
LANG = ROOT / "src/main/resources/assets/quarkos_emotes/lang/en_us.json"
BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "prop", "table"}
CATEGORIES = {"greeting", "reaction", "dance", "pose", "fun"}
RESOURCE_ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z")
TIME = re.compile(r"(?:0|[1-9][0-9]*)(?:\.[0-9]+)?\Z")


class Invalid(ValueError):
    """A content error, reported with its filename by the command line tool."""


def require(condition, message):
    if not condition:
        raise Invalid(message)


def pairs_without_duplicates(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"duplicate JSON key {key!r}")
        result[key] = value
    return result


def read_json(path):
    def reject_constant(value):
        raise Invalid(f"non-finite JSON number {value}")

    return json.loads(path.read_text(encoding="utf-8"),
                      object_pairs_hook=pairs_without_duplicates,
                      parse_constant=reject_constant)


def number(value):
    return type(value) in (int, float) and math.isfinite(value)


def obj(value, where, required=(), allowed=None):
    require(isinstance(value, dict), f"{where}: expected an object")
    require(set(required) <= value.keys(), f"{where}: missing {sorted(set(required) - value.keys())}")
    if allowed is not None:
        require(value.keys() <= set(allowed), f"{where}: unsupported fields {sorted(value.keys() - set(allowed))}")


def vec3(value, where):
    require(isinstance(value, list) and len(value) == 3 and all(number(n) for n in value),
            f"{where}: expected three finite numbers (Molang is not supported)")


def resource(value, where):
    require(isinstance(value, str) and RESOURCE_ID.fullmatch(value),
            f"{where}: expected a namespaced resource id")


def timed_items(value, length, where):
    obj(value, where)
    require(bool(value), f"{where}: empty timeline")
    previous = -1.0
    for key, item in value.items():
        require(isinstance(key, str) and TIME.fullmatch(key), f"{where}: invalid time {key!r}")
        time = float(key)
        require(math.isfinite(time) and previous <= time <= length,
                f"{where}: times must not decrease and must stay in [0, {length}] ({key})")
        previous = time
        yield key, item


def channel(value, length, where):
    def vector(value, label):
        if where.endswith(".scale") and number(value):
            return
        vec3(value, label)
    if number(value):
        vector(value, where)
        return 1
    if isinstance(value, list):
        vector(value, where)
        return 1
    count = 0
    for time, frame in timed_items(value, length, where):
        label = f"{where}@{time}"
        if isinstance(frame, list) or number(frame):
            vector(frame, label)
        else:
            obj(frame, label, allowed={"pre", "post", "lerp_mode"})
            require("pre" in frame or "post" in frame, f"{label}: expected pre or post")
            for side in ("pre", "post"):
                if side in frame:
                    vector(frame[side], label + "." + side)
            require(frame.get("lerp_mode", "linear") in ("linear", "catmullrom", "step"),
                    f"{label}: unknown lerp_mode")
        count += 1
    return count


def metadata(value, inferred_id, length):
    obj(value, "quarkos", allowed={"id", "name", "icon", "category", "cancel_on_move",
                                  "easing", "sound", "prop", "notes", "arm_pivot"})
    if "arm_pivot" in value:
        require(value["arm_pivot"] in ("model", "steve"), "quarkos.arm_pivot: expected model or steve")
    if "id" in value:
        resource(value["id"], "quarkos.id")
        require(value["id"] == inferred_id, "quarkos.id must match the emote id")
    if "name" in value:
        require(isinstance(value["name"], str) and bool(value["name"].strip()),
                "quarkos.name: expected a nonempty display name or translation key")
    if "icon" in value:
        resource(value["icon"], "quarkos.icon")
    if "category" in value:
        require(isinstance(value["category"], str) and value["category"] in CATEGORIES,
                "quarkos.category: unknown category")
    if "cancel_on_move" in value:
        require(type(value["cancel_on_move"]) is bool, "quarkos.cancel_on_move: expected a boolean")
    if "easing" in value:
        require(value["easing"] in ("linear", "ease_in_out"), "quarkos.easing: unknown easing")
    if "notes" in value:
        require(isinstance(value["notes"], str), "quarkos.notes: expected a string")
    if "sound" in value:
        sound = value["sound"]
        obj(sound, "quarkos.sound", ("id",), {"id", "volume", "pitch", "at"})
        resource(sound["id"], "quarkos.sound.id")
        for key, default in (("volume", 1.0), ("pitch", 1.0), ("at", 0.0)):
            require(number(sound.get(key, default)), f"quarkos.sound.{key}: expected a finite number")
        require(sound.get("volume", 1.0) >= 0, "quarkos.sound.volume must be nonnegative")
        require(sound.get("pitch", 1.0) > 0, "quarkos.sound.pitch must be positive")
        require(0 <= sound.get("at", 0.0) <= length, "quarkos.sound.at is outside the animation")
    if "prop" in value:
        prop = value["prop"]
        obj(prop, "quarkos.prop")
        if "item" in prop:
            obj(prop, "quarkos.prop", ("item",), {"item"})
            resource(prop["item"], "quarkos.prop.item")
        else:
            obj(prop, "quarkos.prop", ("model", "cubes", "bone", "attach"),
                {"model", "cubes", "bone", "attach"})
            require(prop["model"] == "cubes", "quarkos.prop.model must be cubes")
            require(prop["bone"] == "prop", "quarkos.prop.bone must be prop")
            require(prop["attach"] in ("right_arm", "body", "world"), "quarkos.prop.attach: unknown attachment")
            cubes = prop["cubes"]
            require(isinstance(cubes, list) and bool(cubes), "quarkos.prop.cubes must be a nonempty array")
            for index, cube in enumerate(cubes):
                where = f"quarkos.prop.cubes[{index}]"
                obj(cube, where, ("from", "to", "texture"), {"from", "to", "texture"})
                vec3(cube["from"], where + ".from")
                vec3(cube["to"], where + ".to")
                require(all(a < b for a, b in zip(cube["from"], cube["to"])),
                        f"{where}: each from coordinate must be less than to")
                resource(cube["texture"], where + ".texture")


def animation_file(path):
    return path.suffix == ".json" and not path.name.endswith((".geo.json", ".quarkos.json"))


def discover_animation(path, root):
    relative = path.relative_to(root)
    if any(part.startswith("_") for part in relative.parts[:-1]):
        return False
    if len(relative.parts) == 1:
        return animation_file(path)
    return path.name == path.parent.name + ".animation.json"


def companions(path):
    if path.name != path.parent.name + ".animation.json":
        return []
    return [p for suffix in ("_enter", "_loop")
            if (p := path.with_name(path.parent.name + suffix + ".animation.json")).is_file()]


def stem(path):
    return path.name.removesuffix(".json").removesuffix(".animation")


def validate_geometry(path):
    data = read_json(path)
    obj(data, "geo", ("format_version", "minecraft:geometry"))
    require(data["format_version"] == "1.12.0", "geo format_version must be 1.12.0")
    require(isinstance(data["minecraft:geometry"], list) and data["minecraft:geometry"], "empty geometry")
    names = set()
    for geo in data["minecraft:geometry"]:
        obj(geo, "geometry", ("description", "bones"))
        desc = geo["description"]
        for key in ("texture_width", "texture_height"):
            require(type(desc.get(key)) is int and desc[key] > 0, "invalid " + key)
        local_names = set()
        for bone in geo["bones"]:
            obj(bone, "geo bone", ("name",))
            require(isinstance(bone["name"], str) and bone["name"] not in local_names, "duplicate/invalid geo bone")
            local_names.add(bone["name"])
            names.add(bone["name"])
            for key in ("pivot", "rotation"):
                vec3(bone.get(key, [0, 0, 0]), "geo bone." + key)
            for cube in bone.get("cubes", []):
                obj(cube, "cube", ("origin", "size", "uv"))
                for key in ("origin", "size", "pivot", "rotation"):
                    vec3(cube.get(key, [0, 0, 0]), "cube." + key)
                require(all(n >= 0 for n in cube["size"]), "negative cube size")
                require(isinstance(cube["uv"], list) and len(cube["uv"]) == 2
                        and all(type(n) is int for n in cube["uv"]), "cube.uv: expected box UV [u, v]")
        parents={b["name"]:b.get("parent") for b in geo["bones"]}
        for bone in parents:
            visited=set()
            cursor=bone
            while cursor is not None:
                require(cursor not in visited, "geometry parent cycle")
                visited.add(cursor)
                parent=parents[cursor]
                if parent is None or parent in BONES - {"prop", "table"}:
                    break
                require(isinstance(parent,str) and parent in parents, "missing geometry parent " + str(parent))
                cursor=parent
    png = path.with_name(path.name.removesuffix(".geo.json") + ".png")
    require(png.is_file(), f"missing prop texture {png}")
    header = png.read_bytes()[:24]
    require(len(header) == 24 and header[:8] == b"\x89PNG\r\n\x1a\n", "invalid PNG")
    import struct
    width, height = struct.unpack(">II", header[16:24])
    for geo in data["minecraft:geometry"]:
        require((width, height) == (geo["description"]["texture_width"], geo["description"]["texture_height"]), "PNG/geo dimension mismatch")
    return names


def validate(path):
    sidecar = path.with_name(stem(path) + ".quarkos.json")
    side = read_json(sidecar) if sidecar.exists() else None
    prop_bones = set()
    for geo in path.parent.glob("*.geo.json"):
        if geo.with_name(geo.name.removesuffix(".geo.json")+".png").is_file():
            prop_bones.update(validate_geometry(geo))
    data = read_json(path)
    result = validate_data(data, path.name, sidecar=side, prop_bones=prop_bones)
    animation = next(iter(data["animations"].values()))
    extras = set(animation["bones"]) - (BONES - {"prop", "table"})
    legacy = animation.get("quarkos", data.get("quarkos", side or {})).get("prop")
    require(extras <= prop_bones or bool(legacy), f"unbound prop bones: {sorted(extras - prop_bones)}")
    for chain in companions(path):
        clip = read_json(chain)
        validate_data(clip, chain.name, prop_bones=prop_bones)
        mode = next(iter(clip["animations"].values())).get("loop", False)
        require(mode == (False if "_enter.animation.json" in chain.name else True), "invalid chain loop mode")
    return result


def validate_data(data, filename, sidecar=None, prop_bones=(), namespace="quarkos_emotes"):
    """Validate an in-memory document without creating a temporary file."""
    path = Path(filename)
    require(animation_file(path), "filename must be an animation JSON")
    obj(data, "root", ("format_version", "animations"), {"format_version", "animations", "quarkos"})
    require(data["format_version"] == "1.8.0", "format_version must be 1.8.0")
    animations = data["animations"]
    obj(animations, "animations")
    require(len(animations) == 1, "expected exactly one animation per file")
    key, animation = next(iter(animations.items()))
    require(isinstance(key, str) and key.startswith("animation."), "animation key must start with animation.")
    obj(animation, key, ("animation_length", "loop", "bones"),
        {"animation_length", "loop", "bones", "sound_effects", "quarkos"})
    q = animation.get("quarkos", data.get("quarkos", sidecar or {}))
    inferred_id = q.get("id", f"{namespace}:{stem(path)}")
    resource(inferred_id, "emote id")
    length = animation["animation_length"]
    require(number(length) and 0 < length <= 3600,
            "animation_length must be a finite number in (0, 3600]")
    loop = animation["loop"]
    require(type(loop) is bool or loop == "hold_on_last_frame", "loop must be true, false, or hold_on_last_frame")
    bones = animation["bones"]
    obj(bones, "bones", allowed=BONES | set(prop_bones))
    require(bool(bones), "bones must not be empty")
    frames = 0
    for bone, tracks in bones.items():
        obj(tracks, f"bones.{bone}", allowed={"rotation", "position", "scale"})
        require(bool(tracks), f"bones.{bone}: expected rotation, position or scale")
        for kind, values in tracks.items():
            frames += channel(values, length, f"bones.{bone}.{kind}")
    if "sound_effects" in animation:
        for time, event in timed_items(animation["sound_effects"], length, "sound_effects"):
            for sound in event if isinstance(event, list) else [event]:
                obj(sound, f"sound_effects@{time}", ("effect",), {"effect"})
                resource(sound["effect"], f"sound_effects@{time}.effect")
    metadata(q, inferred_id, length)
    name_key = q.get("name")
    return inferred_id, length, str(loop).lower(), len(bones), frames, name_key


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", type=Path,
                        help="animation files or directories; defaults to the bundled emotes")
    args = parser.parse_args(argv)
    files = set()
    errors = []
    for path in args.paths or [BUILTINS]:
        if path.is_dir():
            found = {p for p in path.rglob("*.json") if discover_animation(p, path)}
            if not found:
                errors.append(f"{path}: no JSON animation files found")
            files.update(found)
        elif path.is_file():
            files.add(path)
        else:
            errors.append(f"{path}: does not exist")
    rows = []
    seen = {}
    for path in sorted(files):
        try:
            row = validate(path)
            require(row[0] not in seen, f"duplicate id {row[0]} (also in {seen.get(row[0])})")
            seen[row[0]] = path
            rows.append(row)
        except (Invalid, OSError, ValueError, OverflowError) as error:
            errors.append(f"{path}: {error}")
    if not args.paths:
        try:
            names = read_json(LANG)
            obj(names, str(LANG))
            for row in rows:
                require(row[5] is None or not row[5].startswith("emote.") or
                        (row[5] in names and isinstance(names[row[5]], str) and bool(names[row[5]].strip())),
                        f"missing English name for {row[0]} ({row[5]})")
        except (Invalid, OSError, ValueError) as error:
            errors.append(f"{LANG}: {error}")
    headings = ("id", "length", "loop", "bones", "keyframes")
    table = [headings] + [(row[0], f"{row[1]:g}s", row[2], str(row[3]), str(row[4])) for row in rows]
    widths = [max(len(row[i]) for row in table) for i in range(5)]
    for row in table:
        print("  ".join(value.ljust(width) for value, width in zip(row, widths)))
    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    print(f"\n{len(rows)} valid emotes; {len(errors)} errors.")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
