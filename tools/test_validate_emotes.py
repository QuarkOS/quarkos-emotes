"""Contract checks for folder assets and sidecars; standard-library only."""
from copy import deepcopy
from pathlib import Path
import json
import shutil
import tempfile
import unittest

from tools import validate_emotes as validator
from tools.test_import_blockato import export


class ValidatorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name) / "tableflip"
        shutil.copytree(validator.BUILTINS / "tableflip", self.folder)

    def test_filename_id_ignores_animation_key_namespace(self):
        for filename in ("wave.animation.json", "wave.json"):
            row = validator.validate_data(export("animation.emote.unrelated"), filename)
            self.assertEqual(row[0], "quarkos_emotes:wave")

    def test_sidecar_and_inline_precedence(self):
        row = validator.validate(self.folder / "tableflip.animation.json")
        self.assertEqual(row[0], "quarkos_emotes:tableflip")
        self.assertEqual(row[5], "emote.quarkos_emotes.tableflip")
        data = export()
        data["quarkos"] = {"name": "document"}
        row = validator.validate_data(data, "wave.animation.json", sidecar={"name": "sidecar"})
        self.assertEqual(row[5], "document")
        data["animations"]["animation.player.wave"]["quarkos"] = {"name": "animation"}
        self.assertEqual(validator.validate_data(data, "wave.animation.json", sidecar={"name": "sidecar"})[5], "animation")

    def test_geometry_texture_binding_and_animation_only_discovery(self):
        self.assertEqual(validator.validate_geometry(self.folder / "table_prop.geo.json"), {"table"})
        self.assertFalse(validator.animation_file(self.folder / "table_prop.geo.json"))
        self.assertFalse(validator.animation_file(self.folder / "tableflip.quarkos.json"))
        self.assertTrue(validator.animation_file(self.folder / "tableflip.animation.json"))

    def test_missing_texture_or_geometry_is_rejected(self):
        (self.folder / "table_prop.png").unlink()
        with self.assertRaisesRegex(validator.Invalid, "unbound prop"):
            validator.validate(self.folder / "tableflip.animation.json")
        with self.assertRaisesRegex(validator.Invalid, "missing prop texture"):
            validator.validate_geometry(self.folder / "table_prop.geo.json")
        (self.folder / "table_prop.geo.json").unlink()
        with self.assertRaisesRegex(validator.Invalid, "unbound prop"):
            validator.validate(self.folder / "tableflip.animation.json")

    def test_invalid_uv_pivot_rotation_size_and_png_dimensions_rejected(self):
        geo_path = self.folder / "table_prop.geo.json"
        original = validator.read_json(geo_path)
        for key, value in (("uv", [0]), ("pivot", [0, 0]), ("rotation", [0, "bad", 0]), ("size", [-1, 2, 3])):
            with self.subTest(key=key):
                geo = deepcopy(original)
                geo["minecraft:geometry"][0]["bones"][0]["cubes"][0][key] = value
                geo_path.write_text(json.dumps(geo))
                with self.assertRaises(validator.Invalid):
                    validator.validate_geometry(geo_path)
        original["minecraft:geometry"][0]["description"]["texture_width"] = 32
        geo_path.write_text(json.dumps(original))
        with self.assertRaisesRegex(validator.Invalid, "dimension mismatch"):
            validator.validate_geometry(geo_path)

    def test_orphan_geometry_ignored_and_player_parent_or_cycle_validated(self):
        geo = self.folder / "table_prop.geo.json"
        original = validator.read_json(geo)
        original["minecraft:geometry"][0]["bones"][0]["parent"] = "right_arm"
        geo.write_text(json.dumps(original))
        self.assertEqual(validator.validate_geometry(geo), {"table"})
        for parent in ("unknown", "table"):
            original["minecraft:geometry"][0]["bones"][0]["parent"] = parent
            geo.write_text(json.dumps(original))
            with self.assertRaises(validator.Invalid): validator.validate_geometry(geo)
        # Unpaired files aren't runtime props and must not be parsed.
        (self.folder / "orphan.geo.json").write_text("broken ignored geometry")
        geo.write_bytes((validator.BUILTINS / "tableflip/table_prop.geo.json").read_bytes())
        self.assertEqual(validator.validate(self.folder / "tableflip.animation.json")[0], "quarkos_emotes:tableflip")

    def test_loop_modes_and_sit_full_filename(self):
        for mode in (True, False, "hold_on_last_frame"):
            row = validator.validate_data(export("animation.emote.sit", mode), "sit_full.animation.json")
            self.assertEqual(row[:3], ("quarkos_emotes:sit_full", 2.0, str(mode).lower()))
        for mode in ("true", "false", "yes", 1, None):
            with self.assertRaises(validator.Invalid):
                validator.validate_data(export(loop=mode), "bad.animation.json")

    def test_shipped_catalog_has_exact_forty_two_ids_eight_loops_five_holds(self):
        files = [p for p in validator.BUILTINS.rglob("*.json") if validator.discover_animation(p, validator.BUILTINS)]
        rows = [validator.validate(p) for p in files]
        expected = set("wave thinking clap tableflip facepalm groove bigdance bow salute laugh shrug cheer point dab sit_full stretch yes no cry rage victory heart_hands wave_big backflip spin worm floss tpose zombie_walk slow_clap sleep sit jump_hype fishing_cast sword_salute guitar drums pushups default_dance kneel lay_down meditate".split())
        self.assertEqual({r[0] for r in rows}, {"quarkos_emotes:" + name for name in expected})
        self.assertEqual(len(rows), 42)
        self.assertEqual({r[0] for r in rows if r[2] == "true"}, {"quarkos_emotes:"+n for n in ("groove", "bigdance", "floss", "zombie_walk", "guitar", "drums", "pushups", "default_dance")})
        self.assertEqual({r[0] for r in rows if r[2] == "hold_on_last_frame"}, {"quarkos_emotes:"+n for n in ("sit", "sleep", "kneel", "lay_down", "meditate")})
        for name in expected - {"stretch"}:
            side = validator.read_json(validator.BUILTINS / name / (name+".quarkos.json"))
            self.assertEqual(side.get("arm_pivot", "model"), "steve" if name in {"backflip", "spin", "worm", "sleep", "lay_down", "pushups", "sword_salute"} else "model")
        names = validator.read_json(validator.LANG)
        for row in rows:
            self.assertIn(row[5], names)

    def test_scalar_scale_pre_post_and_equal_numeric_times(self):
        for scale in (0, 1, [1, 0, 1], {"0": 0, "0.25": {"pre": 0, "post": 1}, "0.250": 1}):
            data = export()
            data["animations"]["animation.player.wave"]["bones"]["right_arm"]["scale"] = scale
            validator.validate_data(data, "wave.animation.json")
        data["animations"]["animation.player.wave"]["bones"]["right_arm"]["scale"] = "bad"
        with self.assertRaises(validator.Invalid):
            validator.validate_data(data, "wave.animation.json")

    def test_bad_sidecar_sound_time_and_type_rejected(self):
        side_path = self.folder / "tableflip.quarkos.json"
        original = validator.read_json(side_path)
        for patch in ({"cancel_on_move": "yes"}, {"arm_pivot": "alex"}, {"sound": {"id": "minecraft:block.wood.hit", "at": 100}}):
            with self.subTest(patch=patch):
                data = original | patch
                side_path.write_text(json.dumps(data))
                with self.assertRaises(validator.Invalid):
                    validator.validate(self.folder / "tableflip.animation.json")


if __name__ == "__main__":
    unittest.main()
