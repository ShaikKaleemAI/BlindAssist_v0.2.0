import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "ml" / "open_data"))
import build_dataset as bd  # noqa: E402

CFG = {
    "unified_classes": ["stairs", "pothole", "manhole", "open_drain", "vehicle"],
    "synonyms": {"stairs": ["staircase", "stairs"]},
    "sources": [
        {"id": "potA", "license": "L1", "names": ["pothole"], "map": {"pothole": "pothole"}},
        {"id": "rf", "license": "CC BY 4.0", "map": {"manhole": "manhole", "Drain-Clean-": "open_drain"}, "noisy": ["drain clean"]},
        {"id": "syn", "license": "L3", "use_synonyms": True},
    ],
}

def mk(root, name, data, label):
    root.mkdir(parents=True, exist_ok=True)
    (root / f"{name}.jpg").write_bytes(data)
    (root / f"{name}.txt").write_text(label)

class OpenData(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp()); self.raw = self.tmp / "raw"; self.out = self.tmp / "out"
        for i in range(60):                               # flat YOLO, names from config
            mk(self.raw / "potA", f"p{i}", f"pot{i}".encode(), "0 0.5 0.5 0.2 0.2\n")
        d = self.raw / "rf" / "train"                      # roboflow-like with data.yaml + images/labels dirs
        (self.raw / "rf").mkdir(parents=True, exist_ok=True)
        (self.raw / "rf" / "data.yaml").write_text("names: ['manhole', 'banana', 'drain-clean-']\n")
        for i in range(40):
            (d / "images").mkdir(parents=True, exist_ok=True); (d / "labels").mkdir(parents=True, exist_ok=True)
            (d / "images" / f"r{i}.jpg").write_bytes(f"rf{i}".encode())
            (d / "labels" / f"r{i}.txt").write_text("0 0.4 0.4 0.1 0.1\n1 0.6 0.6 0.1 0.1\n2 0.2 0.2 0.1 0.1\n")
        (self.raw / "rf" / "train" / "images" / "only_banana.jpg").write_bytes(b"banana-only")
        (self.raw / "rf" / "train" / "labels" / "only_banana.txt").write_text("1 0.5 0.5 0.1 0.1\n")
        mk(self.raw / "potA", "dup", b"pot0", "0 0.5 0.5 0.2 0.2\n")                 # same bytes as p0 -> duplicate
        (self.raw / "syn").mkdir(); (self.raw / "syn" / "data.yaml").write_text("names: ['Staircase', 'unicorn']\n")
        mk(self.raw / "syn", "s0", b"stairs0", "0 0.5 0.5 0.3 0.3\n1 0.1 0.1 0.1 0.1\n")

    def test_build(self):
        s = bd.build(self.raw, self.out, CFG)
        self.assertEqual(s["duplicates"], 1)
        self.assertGreaterEqual(s["no_label_dropped"], 1)                 # banana-only image
        self.assertEqual(sum(s["images"].values()), 60 + 40 + 1)
        for sp in ("train", "val", "test"):
            self.assertGreater(s["images"][sp], 0, sp)
        self.assertGreater(s["boxes"]["manhole"]["train"] + s["boxes"]["manhole"]["val"] + s["boxes"]["manhole"]["test"], 0)
        self.assertEqual(s["unmapped"]["rf"]["banana"], 41)               # dropped + counted, never guessed
        self.assertEqual(s["unmapped"]["syn"]["unicorn"], 1)
        self.assertEqual(sum(s["boxes"]["stairs"].values()), 1)           # synonym 'Staircase' -> stairs
        self.assertEqual(sum(s["boxes"]["vehicle"].values()), 0)

    def test_label_ids_match_unified_order(self):
        bd.build(self.raw, self.out, CFG)
        ids = set()
        for lf in (self.out / "labels").rglob("rf_*.txt"):
            ids |= {int(l.split()[0]) for l in lf.read_text().splitlines()}
        self.assertEqual(ids, {2, 3})                                      # manhole=2, open_drain=3
        self.assertEqual((self.out / "hazard_labels.txt").read_text().split(), CFG["unified_classes"])

    def test_outputs_and_noisy_flag(self):
        bd.build(self.raw, self.out, CFG)
        self.assertTrue((self.out / "data.yaml").exists())
        stats = json.load(open(self.out / "STATS.json")); self.assertIn("unified_classes", stats)
        self.assertIn("noisy", (self.out / "MANIFEST.csv").read_text())

    def test_holdout_source_goes_only_to_test(self):
        s = bd.build(self.raw, self.out, CFG, holdout="potA")
        self.assertEqual(s["per_source"]["potA"]["train"], 0); self.assertEqual(s["per_source"]["potA"]["val"], 0)
        self.assertEqual(s["per_source"]["potA"]["test"], 60)

    def test_deterministic(self):
        a = bd.build(self.raw, self.tmp / "o1", CFG, write_files=False)["images"]
        b = bd.build(self.raw, self.tmp / "o2", CFG, write_files=False)["images"]
        self.assertEqual(dict(a), dict(b))

    def test_fetch_dry_run_lists_all_sources(self):
        import subprocess
        r = subprocess.run([sys.executable, str(ROOT / "ml/open_data/fetch_open_data.py"), "--dry-run"], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0); self.assertIn("openimages_v7", r.stdout); self.assertIn("license=CC BY 4.0", r.stdout)

if __name__ == "__main__":
    unittest.main()

class Report(unittest.TestCase):
    def test_pending_when_missing(self):
        sys.path.insert(0, str(ROOT / "ml")); import make_report as mr
        self.assertIn("PENDING", mr.render_stats(None)); self.assertIn("PENDING", mr.render_eval(None))

    def test_eval_flags_below_target_and_missing_and_scope(self):
        import make_report as mr
        ev = {"scope": "open-data test split; NOT field performance", "conf": 0.25, "map50": 0.5, "map50_95": 0.3,
              "per_class": {"pothole": {"recall": 0.95, "precision": 0.8, "ap50": 0.9, "ap50_95": 0.5},
                            "stairs": {"recall": 0.5, "precision": 0.7, "ap50": 0.6, "ap50_95": 0.3},
                            "person": {"recall": 0.2, "precision": 0.9, "ap50": 0.5, "ap50_95": 0.3}}}
        t = mr.render_eval(ev)
        self.assertIn("| pothole | 0.950 | 0.800 | 0.900 | meets |", t)
        self.assertIn("| stairs | 0.500 | 0.700 | 0.600 | BELOW |", t)
        self.assertIn("| person | 0.200 | 0.900 | 0.500 | - |", t)          # not a safety class: no verdict
        self.assertIn("Not evaluated", t); self.assertIn("NOT field", t)

    def test_stats_warns_about_zero_test_boxes(self):
        import make_report as mr
        st = {"images": {"train": 5}, "unified_classes": ["stairs", "pothole"], "boxes": {"pothole": {"test": 3}}}
        self.assertIn("CANNOT be measured", mr.render_stats(st)); self.assertIn("stairs", mr.render_stats(st))

    def test_main_writes_files(self):
        import make_report as mr
        tmp = Path(tempfile.mkdtemp())
        (tmp / "ml/open_data").mkdir(parents=True)
        (tmp / "ml/open_data/sources.yaml").write_text((ROOT / "ml/open_data/sources.yaml").read_text(encoding="utf-8"), encoding="utf-8")
        mr.main(tmp)
        self.assertTrue((tmp / "docs/generated/DATA_LICENSES.md").exists())
        self.assertIn("PENDING", (tmp / "docs/generated/EVAL_RESULTS.md").read_text())
