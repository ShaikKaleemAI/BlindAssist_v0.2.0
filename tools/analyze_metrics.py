"""Summarise metrics.csv from the app (t_s,fps,latency_ms,temp_c,battery_pct,limited).
Compares to targets but only as a report; field validity depends on how the log was recorded."""
import csv
import statistics
import sys

TARGETS = {"fps": 15.0, "latency_ms": 150.0, "temp_c": 43.0}

def summarize(path):
    with open(path) as f:
        rows = list(csv.DictReader(f))
    if not rows:
        return {"status": "PENDING FIELD DATA", "rows": 0}
    fps = [float(r["fps"]) for r in rows]
    lat = [float(r["latency_ms"]) for r in rows]
    temp = [float(r["temp_c"]) for r in rows if r["temp_c"].lower() != "nan"]
    t = [float(r["t_s"]) for r in rows]
    bat = [int(r["battery_pct"]) for r in rows if int(r["battery_pct"]) >= 0]
    lat_s = sorted(lat)
    return {
        "rows": len(rows), "duration_s": t[-1] - t[0],
        "fps_mean": statistics.mean(fps), "fps_min": min(fps),
        "latency_mean_ms": statistics.mean(lat), "latency_p95_ms": lat_s[min(len(lat_s) - 1, int(0.95 * len(lat_s)))],
        "temp_max_c": max(temp) if temp else None,
        "battery_drop_pct": (bat[0] - bat[-1]) if len(bat) > 1 else None,
        "limited_rows": sum(r["limited"].lower() == "true" for r in rows),
        "fps_meets_15": statistics.mean(fps) >= TARGETS["fps"],
        "note": "latency is analyzer-entry to dispatch, NOT photon-to-alert",
    }

if __name__ == "__main__":
    for k, v in summarize(sys.argv[1]).items():
        print(f"{k}: {v}")
