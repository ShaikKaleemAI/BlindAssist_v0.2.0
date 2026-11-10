#!/usr/bin/env bash
# Runner for ablate.py: usage  BA_TAPE=walk_tape.csv python tools/ablation/ablate.py tools/ablation/run_gradle.sh
# $1 = JSON flags. Prints METRIC lines. Needs a tape (see docs/ARCHITECTURE.md) and Gradle/network once.
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${BA_TAPE:?set BA_TAPE to an absolute path of a detection tape CSV}"
if [ -f ./gradlew ]; then G=./gradlew; else G=gradle; fi
BA_FLAGS="$1" $G -q :app:testDebugUnitTest --tests '*ReplayTest.ablationFromEnvironment' --rerun-tasks >/dev/null
cat app/build/ablation_metrics.txt
