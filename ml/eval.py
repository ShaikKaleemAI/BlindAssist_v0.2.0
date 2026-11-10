"""Evaluate on the held-out TEST split and write ml/runs/eval.json (per-class precision/recall/AP).
These are OPEN-DATA benchmark numbers (web photos), NOT field performance for a pocket camera while walking."""
import argparse
import json
from pathlib import Path

from ultralytics import YOLO

p = argparse.ArgumentParser()
p.add_argument("--weights", default="ml/runs/hazard/weights/best.pt")
p.add_argument("--data", default="ml/dataset/data.yaml")
p.add_argument("--conf", type=float, default=0.25)
a = p.parse_args()
m = YOLO(a.weights).val(data=a.data, split="test", plots=True, conf=a.conf)
per = {}
for i, pr, rc, ap50, ap in zip(m.box.ap_class_index, m.box.p, m.box.r, m.box.ap50, m.box.ap):
    per[m.names[int(i)]] = {"precision": float(pr), "recall": float(rc), "ap50": float(ap50), "ap50_95": float(ap)}
out = {"split": "test", "conf": a.conf, "map50": float(m.box.map50), "map50_95": float(m.box.map), "per_class": per,
       "scope": "open-data test split; NOT field performance"}
Path("ml/runs").mkdir(parents=True, exist_ok=True)
json.dump(out, open("ml/runs/eval.json", "w"), indent=1)
for k, v in per.items():
    print(f"{k}: recall@{a.conf}={v['recall']:.3f} precision={v['precision']:.3f}")
