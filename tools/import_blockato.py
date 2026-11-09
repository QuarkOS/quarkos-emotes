#!/usr/bin/env python3
"""Preview or import Blockbench Bedrock exports, preserving native emote metadata."""

from __future__ import annotations

import argparse
from collections import Counter
from copy import deepcopy
from dataclasses import dataclass, field
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shutil
import sys
import tempfile

# Even a preview must not leave Python bytecode beside the tools.
sys.dont_write_bytecode = True

try:
    from . import validate_emotes as validator
except ImportError:
    import validate_emotes as validator


SOURCE = Path("/home/box/blockato/emotes")
BACKUPS = Path(__file__).resolve().parent / "replaced"
PROP_ALIASES = {"table", "prop_table", "item"}


def slug(value):
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", value)
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")


def leaf(value):
    """Remove export suffixes, animation prefix, and namespace segments."""
    value = re.sub(r"(?:\.animation)?\.json$", "", value, flags=re.I)
    return re.split(r"[.:/\\]", value)[-1]


def name_candidates(value, ids):
    normalized = slug(leaf(value))
    compact = normalized.replace("_", "")
    matches = {name for name in ids if name.replace("_", "") == compact}
    # Generic names are useful only when the installed catalog has one variant.
    if not matches and normalized == "dance":
        matches = {name for name in ids if name.startswith("dance_")}
    return matches


def resolve_id(animation_name, source_file, source_root, ids):
    """Prefer the animation name, then filename, then nearest enclosing folder."""
    hints = [animation_name, source_file.name]
    relative = source_file.relative_to(source_root)
    hints.extend(reversed(relative.parts[:-1]))
    ambiguous = set()
    for hint in hints:
        matches = name_candidates(hint, ids)
        if len(matches) == 1:
            return next(iter(matches)), ""
        ambiguous.update(matches)
    if ambiguous:
        return None, "ambiguous name: " + ", ".join(sorted(ambiguous))
    return None, "unmatched animation " + repr(animation_name)


def normalize(document, animation_name, emote_id, existing=None):
    """Copy payloads verbatim except for the animation key and prop bone aliases."""
    result = deepcopy(document)
    animation = result["animations"][animation_name]
    validator.obj(animation, animation_name)
    bones = animation.get("bones")
    validator.obj(bones, "bones")
    renamed = {}
    for name, tracks in bones.items():
        canonical = "prop" if name.lower() in PROP_ALIASES else name
        validator.require(canonical not in renamed,
                          f"prop bone collision: multiple bones map to {canonical!r}")
        renamed[canonical] = tracks
    animation["bones"] = renamed
    result["animations"] = {f"animation.quarkos_emotes.{emote_id}": animation}
    if existing is not None:
        result.pop("quarkos", None)
        if "quarkos" in existing:
            result["quarkos"] = deepcopy(existing["quarkos"])
    else:
        result["quarkos"] = {
            "id": f"quarkos_emotes:{emote_id}",
            "name": f"emote.quarkos_emotes.{emote_id}",
            "icon": "minecraft:paper",
            "category": "fun",
            "cancel_on_move": True,
        }
    warnings = []
    if "prop" in renamed and not result.get("quarkos", {}).get("prop"):
        warnings.append("prop bone retained but no quarkos.prop model is defined")
    return result, warnings


@dataclass
class Entry:
    source: Path
    animation_name: str = ""
    emote_id: str = "-"
    length: str = "-"
    loop: str = "-"
    bones: str = "-"
    keyframes: str = "-"
    status: str = "skipped"
    reason: str = ""
    error: bool = False
    document: dict | None = None
    warnings: list[str] = field(default_factory=list)


def describe(entry, animation):
    if not isinstance(animation, dict):
        return
    entry.length = str(animation.get("animation_length", "-"))
    entry.loop = str(animation.get("loop", "-")).lower()
    bones = animation.get("bones")
    if isinstance(bones, dict):
        entry.bones = str(len(bones))
        entry.keyframes = str(sum(
            1 if isinstance(channel, list) else len(channel)
            for tracks in bones.values() if isinstance(tracks, dict)
            for channel in tracks.values() if isinstance(channel, (list, dict))))


def load_catalog(output):
    catalog = {}
    if not output.exists():
        return catalog
    for path in sorted(output.glob("*.animation.json")):
        row = validator.validate(path)
        emote_id = path.name.removesuffix(".animation.json")
        validator.require(row[0] == f"quarkos_emotes:{emote_id}",
                          f"{path}: expected native quarkos_emotes id")
        catalog[emote_id] = validator.read_json(path)
    return catalog


def plan_import(source, output, allow_new=False, backup_root=BACKUPS):
    catalog = load_catalog(output)
    entries = []
    ignored = 0
    for path in sorted(source.rglob("*.json")):
        # Do not re-ingest output/backups if they happen to be inside the intake.
        if output.resolve() in path.resolve().parents or backup_root.resolve() in path.resolve().parents:
            continue
        try:
            document = validator.read_json(path)
            if not isinstance(document, dict) or "animations" not in document:
                ignored += 1
                continue
            validator.obj(document["animations"], "animations")
            validator.require(bool(document["animations"]), "animations: empty object")
        except (OSError, ValueError, TypeError, OverflowError) as error:
            entries.append(Entry(path, reason=f"invalid file: {error}", error=True))
            continue
        for animation_name, animation in document["animations"].items():
            entry = Entry(path, animation_name)
            describe(entry, animation)
            entries.append(entry)
            emote_id, reason = resolve_id(animation_name, path, source, catalog)
            if emote_id is None:
                if not allow_new or reason.startswith("ambiguous"):
                    entry.reason = reason + ("; use --new to add it" if reason.startswith("unmatched") else "")
                    continue
                emote_id = slug(leaf(animation_name))
                if not emote_id:
                    entry.reason = "cannot generate a nonempty new id"
                    entry.error = True
                    continue
            entry.emote_id = emote_id
            try:
                normalized, warnings = normalize(document, animation_name, emote_id, catalog.get(emote_id))
                row = validator.validate_data(normalized, emote_id + ".animation.json")
                entry.length, entry.loop, entry.bones, entry.keyframes = (
                    f"{row[1]:g}", row[2], str(row[3]), str(row[4]))
                entry.document = normalized
                entry.warnings = warnings
                entry.status = "replaced" if emote_id in catalog else "new"
                entry.reason = f"animation {animation_name!r}"
            except (ValueError, TypeError, OverflowError) as error:
                entry.reason = f"invalid animation {animation_name!r}: {error}"
                entry.error = True
    # Reject every colliding candidate; traversal order must never choose a winner.
    counts = Counter(entry.emote_id for entry in entries if entry.document is not None)
    for entry in entries:
        if entry.document is not None and counts[entry.emote_id] > 1:
            entry.document = None
            entry.status = "skipped"
            entry.reason = "multiple source animations target this id; keep only one export"
            entry.error = True
    return entries, ignored


def write_entry(entry, output, backup):
    target = output / (entry.emote_id + ".animation.json")
    output.mkdir(parents=True, exist_ok=True)
    if target.exists():
        backup.mkdir(parents=True, exist_ok=True)
        shutil.copy2(target, backup / target.name)
    # Finish serialization before touching the destination; replace it atomically.
    payload = json.dumps(entry.document, ensure_ascii=False, indent=2, allow_nan=False) + "\n"
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=output,
                                         prefix=".blockato-", suffix=".tmp", delete=False) as stream:
            temporary = Path(stream.name)
            stream.write(payload)
        os.replace(temporary, target)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def print_report(entries, dry_run):
    table = [("id", "source file", "length (s)", "loop", "bones", "keyframes", "result + reason")]
    for entry in entries:
        reason = entry.reason
        if dry_run and entry.status != "skipped":
            reason = "would " + ("replace" if entry.status == "replaced" else "create") + "; " + reason
        if entry.warnings:
            reason += "; WARNING: " + "; ".join(entry.warnings)
        table.append((entry.emote_id, str(entry.source), entry.length, entry.loop,
                      entry.bones, entry.keyframes, entry.status + ": " + reason))
    widths = [max(len(row[i]) for row in table) for i in range(len(table[0]))]
    for row in table:
        print("  ".join(value.ljust(width) for value, width in zip(row, widths)))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", nargs="?", type=Path, default=SOURCE,
                        help=f"recursive intake directory (default: {SOURCE})")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--apply", action="store_true", help="write validated candidates and back up replacements")
    mode.add_argument("--dry-run", action="store_true", help="preview without writing anything (the default)")
    parser.add_argument("--new", action="store_true", help="also import unmatched names as new emotes")
    parser.add_argument("--output-dir", type=Path, default=validator.BUILTINS)
    parser.add_argument("--backup-dir", type=Path, default=BACKUPS,
                        help="parent for timestamped replacement backups")
    args = parser.parse_args(argv)
    if not args.source.is_dir():
        print(f"ERROR: source directory does not exist: {args.source}", file=sys.stderr)
        return 1
    try:
        entries, ignored = plan_import(args.source, args.output_dir, args.new, args.backup_dir)
    except (OSError, ValueError, TypeError, OverflowError) as error:
        print(f"ERROR: cannot read native catalog: {error}", file=sys.stderr)
        return 1
    backup = args.backup_dir / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    if args.apply:
        for entry in entries:
            if entry.document is not None:
                try:
                    write_entry(entry, args.output_dir, backup)
                except (OSError, ValueError) as error:
                    entry.status, entry.reason, entry.error = "skipped", f"write failed: {error}", True
    print("APPLY" if args.apply else "DRY RUN — no files written")
    print_report(entries, not args.apply)
    counts = Counter(entry.status for entry in entries)
    print(f"\n{counts['replaced']} replaced; {counts['new']} new; {counts['skipped']} skipped; "
          f"{ignored} JSON files without animations ignored.")
    if not entries:
        print("No animation exports found; work-in-progress folders are OK.")
    if args.apply and backup.exists():
        print(f"Replacement backups: {backup}")
    print("\nvalidate_emotes.py: candidates checked in memory; checking existing output files:")
    validation = validator.main([str(args.output_dir)]) if any(args.output_dir.rglob("*.json")) else 0
    return int(bool(validation or any(entry.error for entry in entries)))


if __name__ == "__main__":
    sys.exit(main())
