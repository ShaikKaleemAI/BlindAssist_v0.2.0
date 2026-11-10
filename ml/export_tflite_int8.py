"""Export best.pt to INT8 TFLite. Needs a representative calibration set (real walk frames), else quantization is poor."""
import argparse
from ultralytics import YOLO

p = argparse.ArgumentParser()
p.add_argument("--weights", default="ml/runs/hazard/weights/best.pt")
p.add_argument("--data", default="ml/dataset/data.yaml")
p.add_argument("--imgsz", type=int, default=416)
a = p.parse_args()
out = YOLO(a.weights).export(format="tflite", int8=True, imgsz=a.imgsz, data=a.data)
print("exported:", out, "-> copy to app/src/main/assets/models/hazard_yolo_int8.tflite")
