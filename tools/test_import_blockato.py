"""Synthetic intake tests; no animator files or repository content are modified."""

from copy import deepcopy
from contextlib import redirect_stderr, redirect_stdout
import io
import json
from pathlib import Path
import tempfile
import unittest

try:
    from . import import_blockato as importer
    from . import validate_emotes as validator
except ImportError:
    import import_blockato as importer
    import validate_emotes as validator


def export(name="animation.player.wave", loop=False):
    return {
        "format_version": "1.8.0",
        "animations": {name: {
            "animation_length": 2.0,
            "loop": loop,
            "bones": {"right_arm": {"rotation": {
                "0": {"post": [0, 0, 0], "lerp_mode": "catmullrom"},
                "1": {"pre": [-90, 0, 0], "post": [-95, 0, 0], "lerp_mode": "step"},
                "2": [0, 0, 0],
            }}},
            "sound_effects": {"0.5": {"effect": "minecraft:block.wood.hit"}},
        }},
    }


class ImportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "source"
        self.output = self.root / "native"
        self.backups = self.root / "backups"
        self.source.mkdir()
        self.output.mkdir()
        self.metadata = {
            "id": "quarkos_emotes:wave", "name": "emote.quarkos_emotes.wave",
            "icon": "minecraft:sunflower", "category": "greeting",
            "cancel_on_move": False, "easing": "linear", "notes": "keep me",
            "sound": {"id": "minecraft:block.wood.hit", "at": 1.0},
        }
        self.builtin("wave", self.metadata)

    def write(self, path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data), encoding="utf-8")
        return path

    def builtin(self, name, metadata=None):
        data = export(f"animation.quarkos_emotes.{name}")
        data["quarkos"] = deepcopy(metadata or {"id": f"quarkos_emotes:{name}"})
        return self.write(self.output / f"{name}.animation.json", data)

    def run_cli(self, *flags):
        stream = io.StringIO()
        with redirect_stdout(stream), redirect_stderr(stream):
            result = importer.main([str(self.source), "--output-dir", str(self.output),
                                    "--backup-dir", str(self.backups), *flags])
        return result, stream.getvalue()

    def plan(self, allow_new=False):
        return importer.plan_import(self.source, self.output, allow_new)[0]

    def test_names_namespaces_case_and_aliases(self):
        ids = {"wave", "tableflip", "dance_groove", "dance_elaborate"}
        for name, expected in [("animation.player.WAVE", "wave"),
                               ("animation.quarkos.tableflip", "tableflip"),
                               ("something.any.table-flip", "tableflip"),
                               ("TABLE_FLIP", "tableflip"), ("tableFlip", "tableflip")]:
            with self.subTest(name=name):
                self.assertEqual(importer.resolve_id(name, self.source / "unknown.json", self.source, ids)[0], expected)
        self.assertEqual(importer.name_candidates("dance", {"dance_groove"}), {"dance_groove"})
        name, reason = importer.resolve_id("dance", self.source / "unknown.json", self.source, ids)
        self.assertIsNone(name)
        self.assertIn("ambiguous", reason)

    def test_filename_and_nested_folder_fallback_and_name_precedence(self):
        ids = {"wave", "tableflip"}
        for path in [self.source / "wave.animation.json", self.source / "wave" / "out" / "export.json"]:
            self.assertEqual(importer.resolve_id("animation.bot.unknown", path, self.source, ids)[0], "wave")
        self.assertEqual(importer.resolve_id("animation.player.wave", self.source / "tableflip.json", self.source, ids)[0], "wave")

    def test_prop_names_and_metadata_carryover(self):
        metadata = deepcopy(self.metadata)
        metadata["id"] = "quarkos_emotes:tableflip"
        metadata["prop"] = {"model": "cubes", "bone": "prop", "attach": "world", "cubes": [
            {"from": [-10, 10, -6], "to": [10, 12, 6], "texture": "minecraft:block/oak_planks"},
        ]}
        self.builtin("tableflip", metadata)
        for alias in ("table", "prop_table", "item", "prop"):
            with self.subTest(alias=alias):
                data = export("animation.player.tableflip")
                animation = next(iter(data["animations"].values()))
                animation["bones"][alias] = {"position": [0, 0, -13]}
                original = deepcopy(data)
                self.write(self.source / "nested" / "nested.animation.json", data)
                entry = self.plan()[0]
                self.assertEqual(entry.status, "replaced")
                self.assertEqual(entry.metadata, metadata)
                self.assertNotIn("quarkos", entry.document)
                expected = deepcopy(animation)
                self.assertEqual(entry.document["animations"], {"animation.player.tableflip": expected})
                self.assertEqual(data, original)

    def test_extra_prop_kept_and_warned(self):
        data = export()
        next(iter(data["animations"].values()))["bones"]["item"] = {"rotation": [0, 0, 0]}
        self.write(self.source / "wave.json", data)
        entry = self.plan()[0]
        self.assertIn("item", entry.document["animations"]["animation.player.wave"]["bones"])
        self.assertIn("no geometry", entry.warnings[0])

    def test_loop_variants_and_unchanged_bedrock_payload(self):
        for loop in (True, False, "hold_on_last_frame"):
            with self.subTest(loop=loop):
                data = export(loop=loop)
                self.write(self.source / "wave.json", data)
                entry = self.plan()[0]
                self.assertEqual(entry.document["animations"]["animation.player.wave"],
                                 data["animations"]["animation.player.wave"])
                self.assertEqual(entry.metadata, self.metadata)
                self.assertNotIn("quarkos", entry.document)

    def test_new_by_default_and_ambiguous_dance_skipped_even_with_new(self):
        self.write(self.source / "jump.json", export("animation.player.Jump-Spin"))
        entry = self.plan()[0]
        self.assertEqual(entry.status, "skipped")
        self.assertIn("--new", entry.reason)
        entry = self.plan(True)[0]
        self.assertEqual(entry.emote_id, "jump_spin")
        self.assertEqual(entry.status, "new")
        self.assertEqual(entry.metadata["id"], "quarkos_emotes:jump_spin")
        self.builtin("dance_groove")
        self.builtin("dance_elaborate")
        self.write(self.source / "dance.json", export("animation.player.dance"))
        self.assertIn("ambiguous", self.plan(True)[0].reason)
        code, _ = self.run_cli("--new", "--apply")
        self.assertEqual(code, 0)
        self.assertTrue((self.output / "jump_spin/jump_spin.animation.json").exists())

    def test_invalid_file_and_animation_skipped_valid_sibling_imported(self):
        (self.source / "broken.json").write_text("{", encoding="utf-8")
        data = export("animation.player.wave")
        data["animations"]["animation.bot.bad"] = {"loop": "yes", "bones": {}}
        self.write(self.source / "batch.json", data)
        self.write(self.source / "rig" / "rig.geo.json", {"minecraft:geometry": []})
        (self.source / "log.txt").write_text("not JSON", encoding="utf-8")
        code, report = self.run_cli("--new", "--apply")
        self.assertEqual(code, 1)
        self.assertIn("invalid file", report)
        self.assertIn("invalid animation", report)
        self.assertIn("0 JSON files without animations ignored", report)
        self.assertEqual(json.loads((self.output / "wave/wave.quarkos.json").read_text()), self.metadata)
        self.assertFalse((self.output / "bad/bad.animation.json").exists())

    def test_dry_run_writes_nothing_default_and_explicit(self):
        self.write(self.source / "wave.json", export())
        self.write(self.source / "other.json", export("animation.player.other"))
        def snapshot():
            return {p.relative_to(self.root): p.read_bytes() if p.is_file() else None
                    for p in self.root.rglob("*")}
        before = snapshot()
        for flags in ([], ["--dry-run"], ["--new"]):
            code, report = self.run_cli(*flags)
            self.assertEqual(code, 0)
            self.assertIn("DRY RUN", report)
            self.assertEqual(snapshot(), before)
        self.assertFalse(self.backups.exists())

    def test_apply_backup_is_exact_and_output_validates(self):
        target = self.output / "wave.animation.json"
        before = target.read_bytes()
        self.write(self.source / "wave" / "wave.animation.json", export(loop=True))
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        backups = list(self.backups.glob("*/*.animation.json"))
        self.assertEqual(len(backups), 1)
        self.assertEqual(backups[0].read_bytes(), before)
        self.assertFalse(target.exists())
        target = self.output / "wave/wave.animation.json"
        self.assertNotEqual(target.read_bytes(), before)
        self.assertEqual(target.read_bytes(), (self.source / "wave/wave.animation.json").read_bytes())
        self.assertEqual(validator.validate(target)[2], "true")

    def test_distinct_prop_names_do_not_collide_and_duplicate_targets_skip(self):
        data = export()
        bones = next(iter(data["animations"].values()))["bones"]
        bones.update({"table": {"position": [0, 0, 0]}, "prop": {"position": [1, 0, 0]}})
        self.write(self.source / "collision.json", data)
        entry = self.plan()[0]
        self.assertEqual(entry.status, "replaced")
        self.assertEqual(set(entry.document["animations"]["animation.player.wave"]["bones"]), {"right_arm", "table", "prop"})
        (self.source / "collision.json").unlink()
        self.write(self.source / "one.json", export())
        self.write(self.source / "two.json", export())
        before = (self.output / "wave.animation.json").read_bytes()
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 1)
        self.assertIn("multiple source animations", report)
        self.assertEqual((self.output / "wave.animation.json").read_bytes(), before)
        self.assertFalse(self.backups.exists())

    def test_invalid_shorthand_rejected_without_writes(self):
        for frame in ({"lerp_mode": "linear"}, {"post": [0, 0, "bad"]},
                      {"pre": [0, 0, 0], "post": [0, 0]}, {"post": [0, 0, 0], "extra": 1}):
            with self.subTest(frame=frame):
                data = export()
                data["animations"]["animation.player.wave"]["bones"]["right_arm"]["rotation"]["0"] = frame
                self.write(self.source / "wave.json", data)
                self.assertEqual(self.plan()[0].status, "skipped")

    def test_folder_assets_exact_copy_sidecar_and_complete_backup(self):
        import shutil
        shipped = validator.BUILTINS / "tableflip"
        shutil.copytree(shipped, self.source / "tableflip")
        (self.source / "tableflip/preview.gif").write_bytes(b"preview")
        (self.source / "tableflip/alex_preview.png").write_bytes(b"proof")
        (self.source / "tableflip/project.bbmodel").write_bytes(b"authoring")
        (self.source / "tableflip/tableflip_clipcheck.txt").write_text("proof")
        code, report = self.run_cli("--new", "--apply")
        self.assertEqual(code, 0, report)
        folder = self.output / "tableflip"
        self.assertEqual(set(p.name for p in folder.iterdir()), {
            "tableflip.animation.json", "tableflip.quarkos.json", "table_prop.geo.json", "table_prop.png"})
        for name in ("tableflip.animation.json", "table_prop.geo.json", "table_prop.png"):
            self.assertEqual((folder / name).read_bytes(), (shipped / name).read_bytes())
        data = json.loads((folder / "tableflip.animation.json").read_text())
        self.assertNotIn("quarkos", data)
        self.assertIn("table", data["animations"]["animation.emote.tableflip"]["bones"])
        self.assertEqual(validator.validate(folder / "tableflip.animation.json")[0], "quarkos_emotes:tableflip")
        # A second replacement backs up PNG, geo, animation and metadata together.
        originals = {p.name: p.read_bytes() for p in folder.iterdir()}
        changed = self.source / "tableflip/tableflip.animation.json"
        changed.write_bytes(changed.read_bytes() + b"\n")
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        for name, payload in originals.items():
            copies = list(self.backups.glob("*/tableflip/" + name))
            self.assertEqual(len(copies), 1)
            self.assertEqual(copies[0].read_bytes(), payload)

    def test_existing_folder_sidecar_kept_when_export_has_none(self):
        flat = self.output / "wave.animation.json"
        flat.unlink()
        self.write(self.output / "wave/wave.animation.json", export())
        self.write(self.output / "wave/wave.quarkos.json", self.metadata)
        source = self.write(self.source / "wave/wave.animation.json", export(loop=True))
        entry = self.plan()[0]
        self.assertEqual(entry.metadata, self.metadata)
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertEqual((self.output / "wave/wave.animation.json").read_bytes(), source.read_bytes())
        self.assertEqual(json.loads((self.output / "wave/wave.quarkos.json").read_text()), self.metadata)

    def test_apply_alone_adds_new_folder_preserves_sidecar_and_skips_unchanged(self):
        source = self.write(self.source / "new_move/new_move.animation.json",
                            export("animation.emote.new_move"))
        # Deliberate formatting must survive the copy.
        source.write_bytes(source.read_bytes() + b"\n\n")
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        folder = self.output / "new_move"
        self.assertEqual((folder / "new_move.animation.json").read_bytes(), source.read_bytes())
        side = folder / "new_move.quarkos.json"
        metadata = json.loads(side.read_text())
        self.assertEqual(metadata["name"], "New Move")
        self.assertTrue(metadata["cancel_on_move"])
        side.write_text(json.dumps(metadata | {"icon": "minecraft:diamond", "name": "My Move"}, indent=4))
        exact_side = side.read_bytes()
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertIn("unchanged", report)
        self.assertEqual(side.read_bytes(), exact_side)
        self.assertFalse(self.backups.exists())
        source.write_bytes(source.read_bytes() + b"\n")
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertEqual(side.read_bytes(), exact_side)
        self.assertEqual((folder / "new_move.animation.json").read_bytes(), source.read_bytes())

    def test_strict_folder_rule_archives_and_chain_bytes(self):
        canonical = self.write(self.source / "sit/sit.animation.json", export("animation.emote.sit", "hold_on_last_frame"))
        for name in ("sit_v1", "sit_v2", "clipcheck", "random"):
            self.write(self.source / f"sit/{name}.animation.json", export("animation.emote.sit"))
        self.write(self.source / "_archive/sit/sit.animation.json", export("animation.emote.sit"))
        self.write(self.source / "sit/_archive/old/old.animation.json", export("animation.emote.sit"))
        loop = self.write(self.source / "sit/sit_loop.animation.json", export("animation.emote.sit_loop", True))
        enter = self.write(self.source / "sit/sit_enter.animation.json", export("animation.emote.sit_enter", False))
        (self.source / "sit/broken_clipcheck.json").write_text("invalid ignored")
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        folder = self.output / "sit"
        self.assertEqual({p.name for p in folder.iterdir()}, {"sit.animation.json", "sit_loop.animation.json", "sit_enter.animation.json", "sit.quarkos.json"})
        for source in (canonical, loop, enter):
            self.assertEqual((folder / source.name).read_bytes(), source.read_bytes())
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertIn("unchanged", report)
        enter.unlink()
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertFalse((folder / enter.name).exists())
        self.assertTrue(list(self.backups.glob("*/sit/sit_enter.animation.json")))

    def test_removed_prop_pair_is_backed_up_and_removed(self):
        import shutil
        shutil.copytree(validator.BUILTINS / "tableflip", self.source / "tableflip")
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        folder = self.output / "tableflip"
        old_geo = (folder / "table_prop.geo.json").read_bytes()
        # A revised animation no longer has a prop.
        self.write(self.source / "tableflip/tableflip.animation.json", export("animation.emote.tableflip"))
        (self.source / "tableflip/table_prop.geo.json").unlink()
        (self.source / "tableflip/table_prop.png").unlink()
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertFalse((folder / "table_prop.geo.json").exists())
        self.assertFalse((folder / "table_prop.png").exists())
        self.assertEqual(next(self.backups.glob("*/tableflip/table_prop.geo.json")).read_bytes(), old_geo)

    def test_include_limits_intake_when_later_batches_are_present(self):
        self.write(self.source / "backflip/backflip.animation.json", export("animation.emote.backflip"))
        self.write(self.source / "later/later.animation.json", export("animation.emote.later"))
        code, report = self.run_cli("--apply", "--include", "quarkos_emotes:backflip,wave")
        self.assertEqual(code, 0, report)
        self.assertTrue((self.output / "backflip/backflip.animation.json").exists())
        self.assertFalse((self.output / "later").exists())
        self.assertIn("outside --include selection", report)

    def test_md5_ledger_and_recopy_unchanged_exports(self):
        import hashlib
        source = self.write(self.source / "wave/wave.animation.json", export())
        digest = hashlib.md5(source.read_bytes()).hexdigest()
        ledger = self.source / "md5_delivered.txt"
        ledger.write_text(f"{digest}  wave/wave.animation.json\n")
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertIn("MD5: 1/1", report)
        target = self.output / "wave/wave.animation.json"
        # The complete current delivery is recopied even on repeated apply.
        import os
        os.utime(target, (1, 1))
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertGreater(target.stat().st_mtime, 1)
        self.assertEqual(target.read_bytes(), source.read_bytes())
        ledger.write_text("0" * 32 + "  wave/wave.animation.json\n")
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 1)
        self.assertIn("MD5 FAIL", report)

    def test_reference_rigs_and_tools_never_imported(self):
        for folder in ("rig", "tools", "_archive"):
            self.write(self.source / folder / "wave/wave.animation.json", export())
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        self.assertIn("No animation exports found", report)

    def test_empty_wip_directory_is_successful(self):
        self.write(self.source / "rig" / "player.geo.json", {"minecraft:geometry": []})
        (self.source / "wave").mkdir()
        code, report = self.run_cli()
        self.assertEqual(code, 0)
        self.assertIn("No animation exports found", report)


if __name__ == "__main__":
    unittest.main()
