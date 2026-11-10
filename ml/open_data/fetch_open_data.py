"""Download open datasets listed in sources.yaml into ml/open_data/raw/<id>/ .
NEEDS NETWORK. UNRUN in the authoring sandbox (it had no network): treat the first run as a smoke test.
Roboflow needs ROBOFLOW_API_KEY (free account). Open Images uses FiftyOne (pip install fiftyone)."""
import argparse
import io
import os
import sys
import urllib.request
import zipfile
from pathlib import Path

import yaml

HERE = Path(__file__).parent

def plan(cfg, only):
    return [s for s in cfg["sources"] if not only or s["id"] in only]

def fetch_url_zip(s, out):
    print(f"[{s['id']}] downloading {s['url']}")
    data = urllib.request.urlopen(s["url"], timeout=120).read()
    zipfile.ZipFile(io.BytesIO(data)).extractall(out)

def fetch_roboflow(s, out):
    key = os.environ.get("ROBOFLOW_API_KEY")
    if not key:
        print(f"[{s['id']}] SKIP: set ROBOFLOW_API_KEY"); return False
    from roboflow import Roboflow
    proj = Roboflow(api_key=key).workspace(s["workspace"]).project(s["project"])
    ver = s.get("version")
    if ver is None:
        try:
            ver = max(int(v.version) for v in proj.versions())
        except Exception:
            ver = 1
            print(f"[{s['id']}] could not list versions, trying version 1")
    proj.version(ver).download("yolov8", location=str(out))
    return True

def fetch_openimages(s, out):
    import fiftyone as fo
    import fiftyone.zoo as foz
    for split, fosplit in (("train", "train"), ("val", "validation"), ("test", "test")):
        for oi_class in s["oi_classes"]:
            try:
                ds = foz.load_zoo_dataset("open-images-v7", split=fosplit, label_types=["detections"],
                                          classes=[oi_class], max_samples=s["max_per_class"] if split == "train" else 200)
                ds.export(export_dir=str(out / "_tmp" / oi_class.replace(" ", "_")), dataset_type=fo.types.YOLOv5Dataset,
                          label_field="ground_truth", classes=[oi_class], split=split)
            except Exception as e:
                print(f"[{s['id']}] WARNING class '{oi_class}' ({split}): {e}")
    # merge per-class exports: each export has its own single-class names list; build_dataset reads each sub-dir's yaml.

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(HERE / "raw"))
    ap.add_argument("--only", nargs="*")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    cfg = yaml.safe_load(open(HERE / "sources.yaml", encoding="utf-8"))
    for s in plan(cfg, a.only):
        out = Path(a.out) / s["id"]
        print(f"[{s['id']}] method={s['method']} license={s['license']} -> {out}")
        if a.dry_run:
            continue
        out.mkdir(parents=True, exist_ok=True)
        try:
            if s["method"] == "url_zip": fetch_url_zip(s, out)
            elif s["method"] == "roboflow": fetch_roboflow(s, out)
            elif s["method"] == "fiftyone_oi": fetch_openimages(s, out)
            elif s["method"] == "manual": print(f"[{s['id']}] MANUAL: {s['instructions']}")
        except Exception as e:
            print(f"[{s['id']}] FAILED: {e}", file=sys.stderr)

if __name__ == "__main__":
    main()
