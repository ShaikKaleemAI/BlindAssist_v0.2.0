# Model card
- Shipped: none in repo. `tools/fetch_models.sh` fetches Google's EfficientDet-Lite0 (COCO, MediaPipe). Accuracy for this use: UNVERIFIED.
- Custom hazard model: NOT TRAINED. Metrics: PENDING FIELD DATA.
- Limits: COCO has no stairs/drops/potholes/glass; trained on non-Indian-street data (ASSUMED bias).

## Custom hazard model (runtime support added, model NOT trained)
Expected files: `app/src/main/assets/models/hazard_yolo_int8.tflite` + `hazard_labels.txt` (generated at ml/dataset/hazard_labels.txt, one name per line in data.yaml order). Output must be YOLOv8-style [1, 4+nc, anchors] or [1, anchors, 4+nc]. Metrics: PENDING FIELD DATA.
## Depth: no model shipped. TODO(user): export Depth Anything V2 small / MiDaS to INT8 TFLite and implement `DepthEstimator`.

## Training + evaluation (pipeline built, NOT RUN)
`ml/run_all.sh` / `ml/train_colab.ipynb`: open data -> YOLOv8n @416 -> INT8 TFLite -> `ml/eval.py` -> docs/generated/EVAL_RESULTS.md. Until that file shows real numbers, every accuracy claim stays PENDING. Even then, results are an open-data benchmark, not field recall/false-alarm rates.
