"""Merge downloaded open datasets into ONE YOLO dataset with the unified hazard classes.
Pure Python (no image decoding) and unit-tested with mock data (tools/tests/test_open_data.py).
- Class names are normalised and mapped via sources.yaml; unmapped classes are DROPPED and counted, never guessed.
- Images left with no labels are dropped (their objects may be unlabeled -> would teach false negatives).
- Exact duplicate files across sources are removed (SHA1).
- Split is a deterministic hash split. --holdout-source puts a whole source into TEST only (cross-domain estimate).
- Writes data.yaml, hazard_labels.txt, MANIFEST.csv (per-image source + licence) and STATS.json (real counts for the dataset card)."""
import argparse
import csv
import hashlib
import json
import re
import shutil
from collections import Counter, defaultdict
from pathlib import Path

import yaml

HERE = Path(__file__).parent
IMG_EXT = {".jpg", ".jpeg", ".png"}

def norm(name):
    return re.sub(r"\s+", " ", re.sub(r"[-_]+", " ", str(name).lower())).strip()

def build_synonym_map(cfg):
    m = {}
    for unified, words in cfg.get("synonyms", {}).items():
        for w in words:
            m[norm(w)] = unified
    return m

def source_map(src, cfg):
    m = {norm(k): v for k, v in (src.get("map") or {}).items()}
    if src.get("use_synonyms"):
        for k, v in build_synonym_map(cfg).items():
            m.setdefault(k, v)
    return m

def read_names(root, src):
    for y in ("data.yaml", "dataset.yaml"):
        p = root / y
        if p.exists():
            names = yaml.safe_load(open(p, encoding="utf-8")).get("names")
            if isinstance(names, dict):
                return [names[k] for k in sorted(names)]
            if names:
                return list(names)
    return list(src.get("names") or [])

def iter_pairs(root):
    for img in sorted(root.rglob("*")):
        if img.suffix.lower() not in IMG_EXT:
            continue
        cands = [img.with_suffix(".txt")]
        parts = list(img.parts)
        if "images" in parts:
            i = len(parts) - 1 - parts[::-1].index("images")
            cands.append(Path(*parts[:i], "labels", *parts[i + 1:]).with_suffix(".txt"))
        lab = next((c for c in cands if c.exists()), None)
        if lab:
            yield img, lab

def split_of(sha):
    v = int(sha[:8], 16) % 100
    return "train" if v < 70 else "val" if v < 85 else "test"

def build(raw, out, cfg, holdout=None, write_files=True):
    raw, out = Path(raw), Path(out)
    unified = cfg["unified_classes"]
    uid = {c: i for i, c in enumerate(unified)}
    seen = set()
    stats = {"images": Counter(), "boxes": defaultdict(Counter), "per_source": defaultdict(Counter),
             "unmapped": defaultdict(Counter), "duplicates": 0, "no_label_dropped": 0}
    manifest = []
    for src in cfg["sources"]:
        root = raw / src["id"]
        if not root.exists():
            continue
        # a source may contain several sub-exports (e.g. one per Open Images class), each with its own names list
        subroots = sorted({p.parent for p in root.rglob("data.yaml")} | {p.parent for p in root.rglob("dataset.yaml")}) or [root]
        oi = {norm(k): v for k, v in (src.get("oi_classes") or {}).items()}
        smap = source_map(src, cfg) if not oi else oi
        noisy = {norm(n) for n in src.get("noisy", [])}
        for sub in subroots:
            names = [norm(n) for n in read_names(sub, src)]
            for img, lab in iter_pairs(sub):
                data = img.read_bytes()
                sha = hashlib.sha1(data).hexdigest()
                if sha in seen:
                    stats["duplicates"] += 1; continue
                lines, used = [], []
                for ln in lab.read_text().splitlines():
                    p = ln.split()
                    if len(p) < 5:
                        continue
                    try:
                        cid = int(p[0]); cname = names[cid]
                    except (ValueError, IndexError):
                        continue
                    target = smap.get(cname)
                    if target is None or target not in uid:
                        stats["unmapped"][src["id"]][cname] += 1; continue
                    lines.append(f"{uid[target]} " + " ".join(p[1:5])); used.append(cname)
                if not lines:
                    stats["no_label_dropped"] += 1; continue
                seen.add(sha)
                split = "test" if holdout and src["id"] == holdout else split_of(sha)
                stats["images"][split] += 1; stats["per_source"][src["id"]][split] += 1
                for ln in lines:
                    stats["boxes"][unified[int(ln.split()[0])]][split] += 1
                name = f"{src['id']}_{sha[:10]}"
                if write_files:
                    (out / "images" / split).mkdir(parents=True, exist_ok=True)
                    (out / "labels" / split).mkdir(parents=True, exist_ok=True)
                    shutil.copy2(img, out / "images" / split / (name + img.suffix.lower()))
                    (out / "labels" / split / (name + ".txt")).write_text("\n".join(lines) + "\n")
                manifest.append([name, src["id"], src["license"], split, ";".join(sorted(set(used))),
                                 "noisy" if any(u in noisy for u in used) else ""])
    if write_files:
        out.mkdir(parents=True, exist_ok=True)
        yaml.safe_dump({"path": str(out), "train": "images/train", "val": "images/val", "test": "images/test",
                        "names": {i: c for i, c in enumerate(unified)}}, open(out / "data.yaml", "w"), sort_keys=False)
        (out / "hazard_labels.txt").write_text("\n".join(unified) + "\n")
        with open(out / "MANIFEST.csv", "w", newline="") as f:
            w = csv.writer(f); w.writerow(["image", "source", "license", "split", "orig_classes", "flag"]); w.writerows(manifest)
        def plain(x):
            return {k: plain(v) for k, v in x.items()} if isinstance(x, dict) else x
        js = plain(stats)
        js["unified_classes"] = unified
        json.dump(js, open(out / "STATS.json", "w"), indent=1)
    return stats

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw", default=str(HERE / "raw"))
    ap.add_argument("--out", default="ml/dataset")
    ap.add_argument("--holdout-source")
    a = ap.parse_args()
    cfg = yaml.safe_load(open(HERE / "sources.yaml", encoding="utf-8"))
    s = build(a.raw, a.out, cfg, a.holdout_source)
    print("images:", dict(s["images"]), "| duplicates removed:", s["duplicates"], "| dropped (no mapped labels):", s["no_label_dropped"])
    for src, c in s["unmapped"].items():
        print(f"UNMAPPED classes in {src} (add to sources.yaml map/synonyms if relevant): {dict(c)}")
    missing = [c for c in cfg["unified_classes"] if not s["boxes"].get(c)]
    if missing:
        print("WARNING classes with ZERO boxes (model cannot learn them):", missing)

if __name__ == "__main__":
    main()
