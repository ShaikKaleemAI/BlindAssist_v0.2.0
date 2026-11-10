"""Train a YOLO hazard detector on the merged open dataset. UNRUN in the authoring sandbox (no GPU/network)."""
import argparse
from ultralytics import YOLO

p = argparse.ArgumentParser()
p.add_argument("--model", default="yolov8n.pt")
p.add_argument("--data", default="ml/dataset/data.yaml")
p.add_argument("--epochs", type=int, default=100)
p.add_argument("--imgsz", type=int, default=416)   # 320 is faster but small hazards (potholes) suffer; measure both
p.add_argument("--batch", type=int, default=32)
a = p.parse_args()
YOLO(a.model).train(data=a.data, epochs=a.epochs, imgsz=a.imgsz, batch=a.batch, project="ml/runs", name="hazard",
                    exist_ok=True, fliplr=0.5, degrees=5, mosaic=1.0)
