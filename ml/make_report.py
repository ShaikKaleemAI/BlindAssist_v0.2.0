"""Turn REAL run outputs into docs/generated/*.md (no numbers are ever written unless the files exist).
Inputs: ml/dataset/STATS.json + MANIFEST.csv (build_dataset.py), ml/runs/eval.json (eval.py)."""
import csv
import json
from pathlib import Path

import yaml

SAFETY = ["stairs", "step", "pothole", "open_drain", "manhole", "curb", "pole", "glass_door", "speed_bump"]
RECALL_TARGET = 0.90

def render_stats(st):
    if not st:
        return "# Dataset statistics\n\nPENDING: dataset not built yet (run ml/open_data/build_dataset.py).\n"
    L = ["# Dataset statistics (generated from ml/dataset/STATS.json)", "",
         f"Images per split: {st.get('images', {})}  |  exact duplicates removed: {st.get('duplicates', 0)}  |  dropped (no mapped labels): {st.get('no_label_dropped', 0)}", "",
         "| class | train | val | test |", "|---|---|---|---|"]
    zero = []
    for c in st.get("unified_classes", []):
        b = st.get("boxes", {}).get(c, {})
        L.append(f"| {c} | {b.get('train', 0)} | {b.get('val', 0)} | {b.get('test', 0)} |")
        if not b.get("test"):
            zero.append(c)
    if zero:
        L += ["", f"**No TEST boxes for: {', '.join(zero)} — recall for these CANNOT be measured.**"]
    L += ["", "Per source (images): " + json.dumps(st.get("per_source", {})), "",
          "Unmapped source classes (dropped): " + json.dumps(st.get("unmapped", {}))]
    return "\n".join(L) + "\n"

def render_eval(ev, st=None):
    if not ev:
        return "# Evaluation\n\nPENDING: ml/eval.py has not been run.\n"
    L = ["# Evaluation on the open-data TEST split (generated from ml/runs/eval.json)", "",
         f"Scope: **{ev.get('scope', 'open-data benchmark')}**. Confidence threshold {ev.get('conf')}. mAP50={ev.get('map50', 0):.3f}, mAP50-95={ev.get('map50_95', 0):.3f}", "",
         f"| class | recall | precision | AP50 | vs recall target {RECALL_TARGET:.2f} (open data only) |", "|---|---|---|---|---|"]
    per = ev.get("per_class", {})
    for c, v in per.items():
        tag = ("meets" if v["recall"] >= RECALL_TARGET else "BELOW") if c in SAFETY else "-"
        L.append(f"| {c} | {v['recall']:.3f} | {v['precision']:.3f} | {v['ap50']:.3f} | {tag} |")
    missing = [c for c in SAFETY if c not in per]
    if missing:
        L += ["", f"**Not evaluated (no test boxes or not learned): {', '.join(missing)}**"]
    L += ["", "These numbers do NOT show field recall or false alarms per 5 minutes: web photos differ from a pocket camera while walking."]
    return "\n".join(L) + "\n"

def render_licenses(cfg, manifest_rows):
    cnt = {}
    for r in manifest_rows:
        cnt[r["source"]] = cnt.get(r["source"], 0) + 1
    L = ["# Data licences (generated)", "", "| source | licence | images used |", "|---|---|---|"]
    for s in cfg["sources"]:
        L.append(f"| {s['id']} | {s['license']} | {cnt.get(s['id'], 0)} |")
    L += ["", "Check each licence (attribution, non-commercial, share-alike) before redistributing the dataset or a model trained on it."]
    return "\n".join(L) + "\n"

def load_json(p):
    return json.load(open(p)) if Path(p).exists() else None

def main(root="."):
    root = Path(root); out = root / "docs" / "generated"; out.mkdir(parents=True, exist_ok=True)
    st = load_json(root / "ml/dataset/STATS.json"); ev = load_json(root / "ml/runs/eval.json")
    cfg = yaml.safe_load(open(root / "ml/open_data/sources.yaml", encoding="utf-8"))
    mp = root / "ml/dataset/MANIFEST.csv"
    rows = list(csv.DictReader(open(mp))) if mp.exists() else []
    (out / "DATASET_STATS.md").write_text(render_stats(st))
    (out / "EVAL_RESULTS.md").write_text(render_eval(ev, st))
    (out / "DATA_LICENSES.md").write_text(render_licenses(cfg, rows))
    print("wrote", [p.name for p in out.glob("*.md")])

if __name__ == "__main__":
    main()
