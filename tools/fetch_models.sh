#!/usr/bin/env bash
# Downloads models into app/src/main/assets/models and verifies SHA256 against tools/models.sha256.
# Large models (>50 MB) are never committed. Needs network once.
set -euo pipefail
cd "$(dirname "$0")/.."
DEST=app/src/main/assets/models
mkdir -p "$DEST"
URL_EFFDET="https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float32/1/efficientdet_lite0.tflite"
fetch() { # name url
  local f="$DEST/$1"
  [ -f "$f" ] || curl -fL --retry 3 -o "$f" "$2"
  local got; got=$(sha256sum "$f" | cut -d' ' -f1)
  local want; want=$(grep " $1\$" tools/models.sha256 2>/dev/null | cut -d' ' -f1 || true)
  if [ -z "$want" ]; then
    echo "TODO(user): no pinned hash for $1. Computed: $got"
    echo "  Review the file, then append '$got  $1' to tools/models.sha256."
    return 0
  fi
  [ "$got" = "$want" ] && echo "OK  $1 sha256 verified" || { echo "FAIL $1 hash mismatch"; rm -f "$f"; exit 1; }
}
fetch efficientdet_lite0.tflite "$URL_EFFDET"
echo "Done. Custom hazard model: place hazard_yolo_int8.tflite in $DEST (see ml/README.md)."
