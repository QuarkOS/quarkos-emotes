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
        ids = {"wave", "table_flip", "dance_groove", "dance_elaborate"}
        for name, expected in [("animation.player.WAVE", "wave"),
                               ("animation.quarkos.tableflip", "table_flip"),
                               ("something.any.table-flip", "table_flip"),
                               ("TABLE_FLIP", "table_flip"), ("tableFlip", "table_flip")]:
            with self.subTest(name=name):
                self.assertEqual(importer.resolve_id(name, self.source / "unknown.json", self.source, ids)[0], expected)
        self.assertEqual(importer.name_candidates("dance", {"dance_groove"}), {"dance_groove"})
        name, reason = importer.resolve_id("dance", self.source / "unknown.json", self.source, ids)
        self.assertIsNone(name)
        self.assertIn("ambiguous", reason)

    def test_filename_and_nested_folder_fallback_and_name_precedence(self):
        ids = {"wave", "table_flip"}
        for path in [self.source / "wave.animation.json", self.source / "wave" / "out" / "export.json"]:
            self.assertEqual(importer.resolve_id("animation.bot.unknown", path, self.source, ids)[0], "wave")
        self.assertEqual(importer.resolve_id("animation.player.wave", self.source / "tableflip.json", self.source, ids)[0], "wave")

    def test_prop_aliases_and_metadata_carryover(self):
        metadata = deepcopy(self.metadata)
        metadata["id"] = "quarkos_emotes:table_flip"
        metadata["prop"] = {"model": "cubes", "bone": "prop", "attach": "world", "cubes": [
            {"from": [-10, 10, -6], "to": [10, 12, 6], "texture": "minecraft:block/oak_planks"},
        ]}
        self.builtin("table_flip", metadata)
        for alias in ("table", "prop_table", "item", "prop"):
            with self.subTest(alias=alias):
                data = export("animation.player.tableflip")
                animation = next(iter(data["animations"].values()))
                animation["bones"][alias] = {"position": [0, 0, -13]}
                original = deepcopy(data)
                self.write(self.source / "nested" / "table.json", data)
                entry = self.plan()[0]
                self.assertEqual(entry.status, "replaced")
                self.assertEqual(entry.document["quarkos"], metadata)
                expected = deepcopy(animation)
                expected["bones"]["prop"] = expected["bones"].pop(alias)
                self.assertEqual(entry.document["animations"], {"animation.quarkos_emotes.table_flip": expected})
                self.assertEqual(data, original)

    def test_extra_prop_kept_and_warned(self):
        data = export()
        next(iter(data["animations"].values()))["bones"]["item"] = {"rotation": [0, 0, 0]}
        self.write(self.source / "wave.json", data)
        entry = self.plan()[0]
        self.assertIn("prop", entry.document["animations"]["animation.quarkos_emotes.wave"]["bones"])
        self.assertIn("no quarkos.prop", entry.warnings[0])

    def test_loop_variants_and_unchanged_bedrock_payload(self):
        for loop in (True, False, "hold_on_last_frame"):
            with self.subTest(loop=loop):
                data = export(loop=loop)
                self.write(self.source / "wave.json", data)
                entry = self.plan()[0]
                self.assertEqual(entry.document["animations"]["animation.quarkos_emotes.wave"],
                                 data["animations"]["animation.player.wave"])
                self.assertEqual(entry.document["quarkos"], self.metadata)

    def test_new_opt_in_and_ambiguous_dance_skipped_even_with_new(self):
        self.write(self.source / "jump.json", export("animation.player.Jump-Spin"))
        entry = self.plan()[0]
        self.assertEqual(entry.status, "skipped")
        self.assertIn("--new", entry.reason)
        entry = self.plan(True)[0]
        self.assertEqual(entry.emote_id, "jump_spin")
        self.assertEqual(entry.status, "new")
        self.assertEqual(entry.document["quarkos"]["id"], "quarkos_emotes:jump_spin")
        self.builtin("dance_groove")
        self.builtin("dance_elaborate")
        self.write(self.source / "dance.json", export("animation.player.dance"))
        self.assertIn("ambiguous", self.plan(True)[0].reason)
        code, _ = self.run_cli("--new", "--apply")
        self.assertEqual(code, 0)
        self.assertTrue((self.output / "jump_spin.animation.json").exists())

    def test_invalid_file_and_animation_skipped_valid_sibling_imported(self):
        (self.source / "broken.json").write_text("{", encoding="utf-8")
        data = export("animation.player.wave")
        data["animations"]["animation.bot.bad"] = {"loop": "yes", "bones": {}}
        self.write(self.source / "batch.json", data)
        self.write(self.source / "rig.geo.json", {"minecraft:geometry": []})
        (self.source / "log.txt").write_text("not JSON", encoding="utf-8")
        code, report = self.run_cli("--new", "--apply")
        self.assertEqual(code, 1)
        self.assertIn("invalid file", report)
        self.assertIn("invalid animation", report)
        self.assertIn("1 JSON files without animations ignored", report)
        self.assertEqual(json.loads((self.output / "wave.animation.json").read_text())["quarkos"], self.metadata)
        self.assertFalse((self.output / "bad.animation.json").exists())

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
        self.write(self.source / "wave" / "export.json", export(loop=True))
        code, report = self.run_cli("--apply")
        self.assertEqual(code, 0, report)
        backups = list(self.backups.glob("*/*.animation.json"))
        self.assertEqual(len(backups), 1)
        self.assertEqual(backups[0].read_bytes(), before)
        self.assertNotEqual(target.read_bytes(), before)
        self.assertEqual(validator.validate(target)[2], "true")

    def test_prop_collisions_and_duplicate_targets_skip(self):
        data = export()
        bones = next(iter(data["animations"].values()))["bones"]
        bones.update({"table": {"position": [0, 0, 0]}, "prop": {"position": [1, 0, 0]}})
        self.write(self.source / "collision.json", data)
        entry = self.plan()[0]
        self.assertEqual(entry.status, "skipped")
        self.assertIn("collision", entry.reason)
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

    def test_empty_wip_directory_is_successful(self):
        self.write(self.source / "rig" / "player.geo.json", {"minecraft:geometry": []})
        (self.source / "wave").mkdir()
        code, report = self.run_cli()
        self.assertEqual(code, 0)
        self.assertIn("No animation exports found", report)


if __name__ == "__main__":
    unittest.main()
