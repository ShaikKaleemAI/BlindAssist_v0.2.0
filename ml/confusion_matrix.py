"""Confusion matrix + per-class recall from a CSV with columns true,pred (use 'none' for missed / background).
Safety hazards are judged by per-class recall, never by overall accuracy."""
import csv
import sys
from collections import Counter

def matrix(pairs):
    classes = sorted({p for pr in pairs for p in pr})
    cnt = Counter(pairs)
    recall = {}
    for c in classes:
        tot = sum(cnt[(c, p)] for p in classes)
        recall[c] = cnt[(c, c)] / tot if tot and c != "none" else None
    return classes, cnt, recall

def main(path):
    pairs = [(r["true"], r["pred"]) for r in csv.DictReader(open(path))]
    if not pairs:
        print("PENDING FIELD DATA: no labelled predictions"); return
    classes, cnt, recall = matrix(pairs)
    print("true\\pred," + ",".join(classes))
    for t in classes:
        print(t + "," + ",".join(str(cnt[(t, p)]) for p in classes))
    for c, r in recall.items():
        if r is not None:
            print(f"recall[{c}]={r:.3f}")

if __name__ == "__main__":
    main(sys.argv[1])
