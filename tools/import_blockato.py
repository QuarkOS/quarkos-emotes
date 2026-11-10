#!/usr/bin/env python3
"""Preview or import Blockbench Bedrock exports, preserving native emote metadata."""

from __future__ import annotations

import argparse
from collections import Counter
from copy import deepcopy
from dataclasses import dataclass, field
from datetime import datetime, timezone
import json
import hashlib
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
PLAYER_BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}


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
    """Split multi-animation files without changing animation keys or bone names."""
    result = deepcopy(document)
    result["animations"] = {animation_name: deepcopy(document["animations"][animation_name])}
    return result, []


def presentation(document, animation_name):
    return document["animations"][animation_name].get("quarkos", document.get("quarkos"))


def assets(folder):
    """Only runtime files are imported: never projects, previews or clip reports."""
    for path in sorted(folder.iterdir()):
        if path.is_file() and ((path.name.endswith(".geo.json") and path.with_name(path.name.removesuffix(".geo.json") + ".png").is_file()) or
                (path.suffix == ".png" and path.with_suffix(".geo.json").is_file())):
            yield path


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
    metadata: dict | None = None
    target: Path | None = None


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
    for path in sorted(output.rglob("*.json")):
        if not validator.discover_animation(path, output):
            continue
        row = validator.validate(path)
        emote_id = validator.stem(path)
        validator.require(row[0] == f"quarkos_emotes:{emote_id}",
                          f"{path}: expected native quarkos_emotes id")
        validator.require(emote_id not in catalog, f"duplicate catalog id {emote_id}")
        document = validator.read_json(path)
        key = next(iter(document["animations"]))
        side = path.with_name(emote_id + ".quarkos.json")
        metadata = presentation(document, key)
        if metadata is None and side.exists():
            metadata = validator.read_json(side)
        catalog[emote_id] = (path, metadata)
    return catalog


def plan_import(source, output, allow_new=True, backup_root=BACKUPS, include=None):
    catalog = load_catalog(output)
    entries = []
    ignored = 0
    for path in sorted(source.rglob("*.json")):
        if any(part in {"_archive", "rig", "tools"} for part in path.relative_to(source).parts[:-1]):
            continue
        # Do not re-ingest output/backups if they happen to be inside the intake.
        if output.resolve() in path.resolve().parents or backup_root.resolve() in path.resolve().parents:
            continue
        if not validator.discover_animation(path, source):
            ignored += 1
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
            if include is not None and emote_id not in include:
                entry.reason = "outside --include selection"
                continue
            try:
                normalized, warnings = normalize(document, animation_name, emote_id)
                side = path.with_name(validator.stem(path) + ".quarkos.json")
                delivered = validator.read_json(side) if side.exists() else None
                existing = catalog.get(emote_id)
                metadata = delivered if delivered is not None else existing[1] if existing and existing[1] is not None else {
                    "id": f"quarkos_emotes:{emote_id}",
                    "name": emote_id.replace("_", " ").title(),
                    "icon": "minecraft:paper", "category": "fun", "cancel_on_move": True,
                }
                prop_bones = set()
                for geo in (p for p in assets(path.parent) if p.name.endswith(".geo.json")):
                    prop_bones.update(validator.validate_geometry(geo))
                for chain in validator.companions(path):
                    clip = validator.read_json(chain)
                    validator.validate_data(clip, chain.name, prop_bones=prop_bones)
                    mode = next(iter(clip["animations"].values())).get("loop", False)
                    validator.require(mode == (False if "_enter.animation.json" in chain.name else True), "invalid chain loop mode")
                bones = animation.get("bones", {})
                extras = set(bones) - PLAYER_BONES
                missing = extras - prop_bones
                if missing and not (metadata or {}).get("prop"):
                    warnings.append("no geometry for prop bones: " + ", ".join(sorted(missing)))
                row = validator.validate_data(normalized, emote_id + ".animation.json",
                                              sidecar=metadata, prop_bones=extras | prop_bones)
                validator.require(row[0] == f"quarkos_emotes:{emote_id}", "metadata id conflicts with destination filename")
                entry.length, entry.loop, entry.bones, entry.keyframes = (
                    f"{row[1]:g}", row[2], str(row[3]), str(row[4]))
                entry.document = normalized
                entry.metadata = metadata
                entry.target = existing[0] if existing else None
                entry.warnings = warnings
                entry.status = "replaced" if emote_id in catalog else "new"
                entry.reason = f"animation {animation_name!r}"
                if existing and existing[0] == output / emote_id / (emote_id + ".animation.json"):
                    staged = runtime_payloads(entry, output)
                    current = {p.name for p in [*assets(existing[0].parent), *validator.companions(existing[0])]}
                    incoming = {p.name for p in [*assets(path.parent), *validator.companions(path)]}
                    if current == incoming and all(dest.is_file() and dest.read_bytes() == content
                                                   for dest, content in staged.items()):
                        entry.status = "unchanged"
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


def runtime_payloads(entry, output):
    folder = output / entry.emote_id
    target = folder / (entry.emote_id + ".animation.json")
    original = validator.read_json(entry.source)
    payload = (entry.source.read_bytes() if original == entry.document else
               (json.dumps(entry.document, ensure_ascii=False, indent=2, allow_nan=False) + "\n").encode("utf-8"))
    staged = {target: payload}
    for asset in [*assets(entry.source.parent), *validator.companions(entry.source)]:
        staged[folder / asset.name] = asset.read_bytes()
    side = folder / (entry.emote_id + ".quarkos.json")
    # Keep existing sidecar formatting as well as its presentation settings.
    staged[side] = (side.read_bytes() if side.is_file() and validator.read_json(side) == entry.metadata else
                    (json.dumps(entry.metadata, ensure_ascii=False, indent=2, allow_nan=False) + "\n").encode("utf-8"))
    return staged


def write_entry(entry, output, backup):
    folder = output / entry.emote_id
    target = folder / (entry.emote_id + ".animation.json")
    prior = entry.target
    if prior is not None and entry.status != "unchanged":
        # Back up the complete old runtime folder (or flat animation + sidecar).
        originals = [prior, prior.with_name(validator.stem(prior) + ".quarkos.json")]
        if prior.parent != output:
            originals.extend(assets(prior.parent))
            originals.extend(validator.companions(prior))
        for original in originals:
            if original.is_file():
                dest = backup / original.relative_to(output)
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(original, dest)
    folder.mkdir(parents=True, exist_ok=True)
    # Single-animation exports are copied byte-for-byte, including formatting.
    staged = runtime_payloads(entry, output)
    for dest, content in staged.items():
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(dir=folder, prefix=".blockato-", suffix=".tmp", delete=False) as stream:
                temporary = Path(stream.name)
                stream.write(content)
            os.replace(temporary, dest)
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)
    # Assets removed by the animator must not linger and bind stale props.
    for asset in [*assets(folder), *validator.companions(target)]:
        if asset not in staged:
            asset.unlink()
    if prior is not None and prior != target:
        prior.unlink()
        prior.with_name(validator.stem(prior) + ".quarkos.json").unlink(missing_ok=True)


def print_report(entries, dry_run):
    table = [("id", "source file", "length (s)", "loop", "bones", "keyframes", "result + reason")]
    for entry in entries:
        reason = entry.reason
        if dry_run and entry.status in {"new", "replaced"}:
            reason = "would " + ("replace" if entry.status == "replaced" else "create") + "; " + reason
        if entry.warnings:
            reason += "; WARNING: " + "; ".join(entry.warnings)
        table.append((entry.emote_id, str(entry.source), entry.length, entry.loop,
                      entry.bones, entry.keyframes, entry.status + ": " + reason))
    widths = [max(len(row[i]) for row in table) for i in range(len(table[0]))]
    for row in table:
        print("  ".join(value.ljust(width) for value, width in zip(row, widths)))


def md5_report(entries, source, output, applied):
    """Check delivered bytes against the ledger and, on apply, installed bytes."""
    ledger_file = source / "md5_delivered.txt"
    ledger = {}
    if ledger_file.is_file():
        for line in ledger_file.read_text().splitlines():
            digest, name = line.split(maxsplit=1)
            ledger[name.strip().removeprefix("*")] = digest
    matches = total = 0
    for entry in entries:
        if entry.document is None:
            continue
        for original in [entry.source, *assets(entry.source.parent), *validator.companions(entry.source)]:
            if original == entry.source and validator.read_json(original) != entry.document:
                print(f"MD5 SPLIT {original.relative_to(source)} (multi-animation export; separate output)")
                continue
            total += 1
            relative = original.relative_to(source).as_posix()
            digest = hashlib.md5(original.read_bytes()).hexdigest()
            target = output / entry.emote_id / (entry.emote_id + ".animation.json" if original == entry.source else original.name)
            ledger_ok = not ledger_file.is_file() or ledger.get(relative) == digest
            bytes_ok = not applied or target.is_file() and target.read_bytes() == original.read_bytes()
            ok = ledger_ok and bytes_ok
            matches += ok
            print(f"MD5 {'MATCH' if ok else 'FAIL'} {digest} {relative}"
                  + (" (ledger unavailable)" if not ledger_file.is_file() else "")
                  + (" (source preview)" if not applied else ""))
    print(f"MD5: {matches}/{total} runtime payloads match" + (" delivery and installed bytes" if applied else " delivery (preview)"))
    return matches == total


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", nargs="?", type=Path, default=SOURCE,
                        help=f"recursive intake directory (default: {SOURCE})")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--apply", action="store_true", help="write validated candidates and back up replacements")
    mode.add_argument("--dry-run", action="store_true", help="preview without writing anything (the default)")
    parser.add_argument("--new", action="store_true", help="compatibility flag; new names are now imported by default")
    parser.add_argument("--include", help="comma-separated emote IDs/names to import from a growing delivery folder")
    parser.add_argument("--output-dir", type=Path, default=validator.BUILTINS)
    parser.add_argument("--backup-dir", type=Path, default=BACKUPS,
                        help="parent for timestamped replacement backups")
    args = parser.parse_args(argv)
    if not args.source.is_dir():
        print(f"ERROR: source directory does not exist: {args.source}", file=sys.stderr)
        return 1
    try:
        include = {name.removeprefix("quarkos_emotes:") for name in args.include.split(",")} if args.include else None
        entries, ignored = plan_import(args.source, args.output_dir, True, args.backup_dir, include)
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
    print(f"\n{counts['replaced']} replaced; {counts['new']} new; {counts['unchanged']} unchanged; {counts['skipped']} skipped; "
          f"{ignored} JSON files without animations ignored.")
    if not entries:
        print("No animation exports found; work-in-progress folders are OK.")
    if args.apply and backup.exists():
        print(f"Replacement backups: {backup}")
    hashes_ok = md5_report(entries, args.source, args.output_dir, args.apply)
    print("\nvalidate_emotes.py: candidates checked in memory; checking existing output files:")
    validation = validator.main([str(args.output_dir)]) if any(args.output_dir.rglob("*.json")) else 0
    return int(bool(validation or not hashes_ok or any(entry.error for entry in entries)))


if __name__ == "__main__":
    sys.exit(main())
