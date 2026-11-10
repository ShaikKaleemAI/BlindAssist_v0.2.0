# Data plan
**Stage A (automatic, no user data needed): open datasets** — `ml/open_data/sources.yaml` lists the sources and licences; `ml/run_all.sh` (or ml/train_colab.ipynb) downloads, merges, trains, evaluates. Real counts appear in docs/generated/DATASET_STATS.md only after you run it (PENDING until then).
Known gaps of open data (do not hide them): no up/down stairs labels (single `stairs` class); few or no open drains, curbs, speed bumps, glass doors; Indian auto-rickshaws only via IDD (registration); mostly web photos, not pocket-height walking video; the `drain*` mapping is noisy.
**Stage B (needed for a 9+ claim): field video** — walking video at pocket height (85 cm trouser / 120 cm shirt), Indian streets, day/dusk/night, rain, multiple phones; label with CVAT/Label Studio; split by SITE; blur faces/plates; consent. Without Stage B, report only open-data results.
Hard negatives: shadows, painted lines, puddles, tiles, reflections. Two annotators on 10% for agreement.
