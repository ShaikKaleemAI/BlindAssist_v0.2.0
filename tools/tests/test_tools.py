import csv
import os
import subprocess
import sys
import tempfile
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(ROOT, "tools"))
sys.path.insert(0, os.path.join(ROOT, "ml"))
import analyze_metrics  # noqa: E402
import confusion_matrix  # noqa: E402

def tmp_csv(header, rows):
    f = tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False, newline="")
    w = csv.writer(f); w.writerow(header); w.writerows(rows); f.close()
    return f.name

class Tools(unittest.TestCase):
    def test_metrics_summary(self):
        p = tmp_csv(["t_s", "fps", "latency_ms", "temp_c", "battery_pct", "limited"],
                    [[0, 16, 80, 35, 90, "false"], [5, 14, 120, 41, 89, "false"], [10, 15, 100, "NaN", 89, "false"]])
        s = analyze_metrics.summarize(p)
        self.assertEqual(s["rows"], 3); self.assertAlmostEqual(s["fps_mean"], 15.0)
        self.assertEqual(s["temp_max_c"], 41.0); self.assertEqual(s["battery_drop_pct"], 1)

    def test_empty_metrics_is_pending(self):
        p = tmp_csv(["t_s", "fps", "latency_ms", "temp_c", "battery_pct", "limited"], [])
        self.assertEqual(analyze_metrics.summarize(p)["status"], "PENDING FIELD DATA")

    def test_confusion_recall(self):
        pairs = [("stairs", "stairs")] * 3 + [("stairs", "none")] + [("pothole", "pothole")]
        _, _, recall = confusion_matrix.matrix(pairs)
        self.assertAlmostEqual(recall["stairs"], 0.75); self.assertAlmostEqual(recall["pothole"], 1.0)
        self.assertIsNone(recall["none"])

    def test_distance_empty_is_pending(self):
        r = subprocess.run([sys.executable, os.path.join(ROOT, "tools/benchmark/analyze_distance.py")],
                           capture_output=True, text=True, cwd=ROOT)
        self.assertIn("PENDING", r.stdout)

    def test_ablation_dry_run_lists_all_configs(self):
        r = subprocess.run([sys.executable, os.path.join(ROOT, "tools/ablation/ablate.py"), "--dry-run"],
                           capture_output=True, text=True)
        self.assertEqual(r.stdout.count("[plan]"), 8)

if __name__ == "__main__":
    unittest.main()
