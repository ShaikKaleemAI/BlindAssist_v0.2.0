#!/usr/bin/env bash
# One command: open data -> merged dataset -> train -> INT8 export -> evaluate -> generated reports -> app assets.
# Needs: network, a GPU (Colab is fine), `pip install -r ml/requirements.txt`, optional ROBOFLOW_API_KEY.
set -euo pipefail
cd "$(dirname "$0")/.."
python ml/open_data/fetch_open_data.py
python ml/open_data/build_dataset.py --out ml/dataset ${HOLDOUT:+--holdout-source $HOLDOUT}
python ml/train.py --epochs "${EPOCHS:-100}"
python ml/export_tflite_int8.py
python ml/eval.py
python ml/make_report.py
# export_tflite_int8.py prints the exported path; copy it (name differs by ultralytics version)
F=$(find ml/runs/hazard/weights -name '*int8*.tflite' | head -1)
[ -n "$F" ] && cp "$F" app/src/main/assets/models/hazard_yolo_int8.tflite && cp ml/dataset/hazard_labels.txt app/src/main/assets/models/hazard_labels.txt
echo "Done. Commit docs/generated/*.md, then rebuild the app. Remember: open-data numbers are not field numbers."
