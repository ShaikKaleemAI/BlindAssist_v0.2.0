"""Split by site so frames from one site never appear in two splits.
Input: a folder with <site>/<frames>.jpg + .txt labels. Output: images|labels/{train,val,test} symlinks."""
import random
import sys
from pathlib import Path

def main(src, dst="ml/dataset", seed=0):
    sites = sorted(p for p in Path(src).iterdir() if p.is_dir())
    random.Random(seed).shuffle(sites)
    n = len(sites)
    cut = {"train": sites[: int(.7 * n)], "val": sites[int(.7 * n): int(.85 * n)], "test": sites[int(.85 * n):]}
    for split, ss in cut.items():
        for s in ss:
            for img in s.glob("*.jpg"):
                for kind, ext, f in (("images", ".jpg", img), ("labels", ".txt", img.with_suffix(".txt"))):
                    if not f.exists():
                        continue
                    out = Path(dst) / kind / split / f"{s.name}_{f.name}"
                    out.parent.mkdir(parents=True, exist_ok=True)
                    if not out.exists():
                        out.symlink_to(f.resolve())
    print({k: len(v) for k, v in cut.items()}, "sites")

if __name__ == "__main__":
    main(*sys.argv[1:])
