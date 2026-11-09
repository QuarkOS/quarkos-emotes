#!/usr/bin/env python3
"""Validate the QuarkOS Emotes Bedrock subset using only Python's standard library."""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
BUILTINS = ROOT / "src/main/resources/assets/quarkos_emotes/emotes"
LANG = ROOT / "src/main/resources/assets/quarkos_emotes/lang/en_us.emotes.json"
BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "prop"}
CATEGORIES = {"greeting", "reaction", "dance", "pose", "fun"}
RESOURCE_ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z")
ANIMATION_ID = re.compile(r"animation\.([a-z0-9_.-]+)\.([a-z0-9_/-]+)\Z")
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
        require(math.isfinite(time) and previous < time <= length,
                f"{where}: times must increase strictly and stay in [0, {length}] ({key})")
        previous = time
        yield key, item


def channel(value, length, where):
    if isinstance(value, list):
        vec3(value, where)
        return 1
    count = 0
    for time, frame in timed_items(value, length, where):
        label = f"{where}@{time}"
        if isinstance(frame, list):
            vec3(frame, label)
        else:
            obj(frame, label, allowed={"pre", "post", "lerp_mode"})
            require("pre" in frame or "post" in frame, f"{label}: expected pre or post")
            for side in ("pre", "post"):
                if side in frame:
                    vec3(frame[side], label + "." + side)
            require(frame.get("lerp_mode", "linear") in ("linear", "catmullrom", "step"),
                    f"{label}: unknown lerp_mode")
        count += 1
    return count


def metadata(value, inferred_id, length):
    obj(value, "quarkos", allowed={"id", "name", "icon", "category", "cancel_on_move",
                                  "easing", "sound", "prop", "notes"})
    if "id" in value:
        resource(value["id"], "quarkos.id")
        require(value["id"] == inferred_id, "quarkos.id must match the animation key")
    if "name" in value:
        require(isinstance(value["name"], str) and bool(value["name"].strip()),
                "quarkos.name: expected a nonempty translation key")
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


def validate(path):
    return validate_data(read_json(path), path.name)


def validate_data(data, filename):
    """Validate an in-memory document without creating a temporary file."""
    path = Path(filename)
    require(path.name.endswith(".animation.json"), "filename must end in .animation.json")
    obj(data, "root", ("format_version", "animations"), {"format_version", "animations", "quarkos"})
    require(data["format_version"] == "1.8.0", "format_version must be 1.8.0")
    animations = data["animations"]
    obj(animations, "animations")
    require(len(animations) == 1, "expected exactly one animation per file")
    key, animation = next(iter(animations.items()))
    match = ANIMATION_ID.fullmatch(key)
    require(match is not None, "animation key must be animation.<namespace>.<name>")
    namespace, name = match.groups()
    inferred_id = f"{namespace}:{name}"
    require(path.name == name.rsplit("/", 1)[-1] + ".animation.json", "filename must match the animation name")
    obj(animation, key, ("animation_length", "loop", "bones"),
        {"animation_length", "loop", "bones", "sound_effects"})
    length = animation["animation_length"]
    require(number(length) and 0 < length <= 3600,
            "animation_length must be a finite number in (0, 3600]")
    loop = animation["loop"]
    require(type(loop) is bool or loop == "hold_on_last_frame", "loop must be true, false, or hold_on_last_frame")
    bones = animation["bones"]
    obj(bones, "bones", allowed=BONES)
    require(bool(bones), "bones must not be empty")
    frames = 0
    for bone, tracks in bones.items():
        obj(tracks, f"bones.{bone}", allowed={"rotation", "position"})
        require(bool(tracks), f"bones.{bone}: expected rotation or position")
        for kind, values in tracks.items():
            frames += channel(values, length, f"bones.{bone}.{kind}")
    if "sound_effects" in animation:
        for time, event in timed_items(animation["sound_effects"], length, "sound_effects"):
            obj(event, f"sound_effects@{time}", ("effect",), {"effect"})
            resource(event["effect"], f"sound_effects@{time}.effect")
    if "quarkos" in data:
        metadata(data["quarkos"], inferred_id, length)
    name_key = data.get("quarkos", {}).get("name")
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
            found = set(path.rglob("*.json"))
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
                require(row[5] in names and isinstance(names[row[5]], str) and bool(names[row[5]].strip()),
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
