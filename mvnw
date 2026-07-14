#!/bin/sh
BASEDIR=$(cd "$(dirname "$0")" && pwd)
MAVEN_HOME="$BASEDIR/.tools/apache-maven-3.9.9"
if [ -x "$MAVEN_HOME/bin/mvn" ]; then
  exec "$MAVEN_HOME/bin/mvn" "$@"
fi
if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi
echo "Maven not found. Extract Apache Maven 3.9.9 to $MAVEN_HOME"
exit 1
