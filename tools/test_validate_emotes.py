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
        with self.assertRaisesRegex(validator.Invalid, "missing prop texture"):
            validator.validate(self.folder / "tableflip.animation.json")
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

    def test_bad_sidecar_sound_time_and_type_rejected(self):
        side_path = self.folder / "tableflip.quarkos.json"
        original = validator.read_json(side_path)
        for patch in ({"cancel_on_move": "yes"}, {"sound": {"id": "minecraft:block.wood.hit", "at": 100}}):
            with self.subTest(patch=patch):
                data = original | patch
                side_path.write_text(json.dumps(data))
                with self.assertRaises(validator.Invalid):
                    validator.validate(self.folder / "tableflip.animation.json")


if __name__ == "__main__":
    unittest.main()
