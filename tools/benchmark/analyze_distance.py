"""Tape-measure distance benchmark: reads the CSV template and reports error per 1 m bin. Prints PENDING if empty."""
import csv
import sys

def main(path="tools/benchmark/distance_template.csv"):
    rows = [r for r in csv.DictReader(open(path)) if r.get("estimated_distance_m")]
    if not rows:
        print("PENDING FIELD DATA: no trials recorded"); return 0
    bins = {}
    for r in rows:
        t = float(r["true_distance_m"]); e = float(r["estimated_distance_m"])
        bins.setdefault(int(t), []).append(abs(e - t) / t)
    for b in sorted(bins):
        v = bins[b]; print(f"{b}-{b+1} m: n={len(v)} mean abs rel err={100*sum(v)/len(v):.1f}%")
    return 0

if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:]))
