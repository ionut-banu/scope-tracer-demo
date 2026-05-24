#!/usr/bin/env bash
#
# Production-style agent run.
#
# This is how you'd trace a real service: attach the agent, start JFR with a
# destination file, and let the agent write the HTML report itself when the
# recording stops. There is NO in-process JFR recording and NO analyzer call
# in the application code -- the agent's FlightRecorderListener does it all.
#
set -euo pipefail
cd "$(dirname "$0")/.."

AGENT_JAR="target/agent/scope-tracer-agent.jar"
JFR_FILE="target/agent-prod.jfr"

# Build, resolve the runtime classpath, and copy the agent fat-jar to a stable
# path (the prepare-package-bound maven-dependency-plugin handles the copy).
mvn -q package
CP="$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout -DincludeScope=runtime | tail -n1)"
CP="target/classes:${CP}"

if [[ ! -f "${AGENT_JAR}" ]]; then
  echo "Agent jar not found at ${AGENT_JAR} -- did 'mvn package' succeed?" >&2
  exit 1
fi

echo "Running AgentDemo with the agent attached and JFR -> ${JFR_FILE}"
echo "(no TracedScope, no analyzer call -- the agent writes the HTML on JFR stop)"
echo

java --enable-preview \
  -javaagent:"${AGENT_JAR}" \
  -XX:StartFlightRecording=filename="${JFR_FILE}",dumponexit=true \
  -classpath "${CP}" \
  com.ionutbanu.scopetracer.demo.AgentDemo

echo
echo "Report written by the agent:"
ls -1 target/agent-prod.html 2>/dev/null \
  && echo "Open target/agent-prod.html in a browser." \
  || echo "Expected target/agent-prod.html -- check stderr for [scope-tracer] messages."
