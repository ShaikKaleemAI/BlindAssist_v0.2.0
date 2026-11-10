"""Per-module ablation harness. Runs RUNNER once per config and collects one metric line per run.
RUNNER must print 'METRIC <name> <value>' lines. No runner exists yet: the replay mode that would be the
runner is NOT IMPLEMENTED (TODO(user)/next run). Use --dry-run to see the plan."""
import json
import subprocess
import sys

MODULES = ["width_cue", "height_cue", "ground_cue", "kalman", "looming_ttc", "avoidance", "hysteresis"]

def configs():
    yield "full", {m: True for m in MODULES}
    for m in MODULES:
        yield f"minus_{m}", {k: (k != m) for k in MODULES}

def main(argv):
    dry = "--dry-run" in argv
    runner = [a for a in argv if not a.startswith("--")]
    out = {}
    for name, flags in configs():
        if dry or not runner:
            print(f"[plan] {name}: disable={[k for k, v in flags.items() if not v]}"); continue
        res = subprocess.run(runner + [json.dumps(flags)], capture_output=True, text=True, check=True)
        out[name] = {l.split()[1]: float(l.split()[2]) for l in res.stdout.splitlines() if l.startswith("METRIC ")}
    if out:
        base = out["full"]
        for name, m in out.items():
            print(name, {k: round(v - base.get(k, 0), 4) for k, v in m.items()})
    elif not dry:
        print("PENDING FIELD DATA: no runner supplied")

if __name__ == "__main__":
    main(sys.argv[1:])
