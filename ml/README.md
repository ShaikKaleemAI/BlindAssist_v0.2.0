# ml/ - hazard model pipeline
Status: pipeline written; dataset-building logic tested on mock data; downloads/training/eval NOT run (authoring sandbox had no network/GPU).
Fastest path: open `ml/train_colab.ipynb` in Google Colab (GPU) -> Run all -> download results.zip -> commit.
Manual: `pip install -r ml/requirements.txt && bash ml/run_all.sh` (env: ROBOFLOW_API_KEY optional, EPOCHS, HOLDOUT=<source id>).
Outputs: ml/dataset (merged YOLO data, MANIFEST.csv, STATS.json), ml/runs/eval.json, docs/generated/*.md, app/src/main/assets/models/hazard_yolo_int8.tflite + hazard_labels.txt.
Open-data results are NOT field results (see ml/dataset/COLLECTION_PLAN.md).
