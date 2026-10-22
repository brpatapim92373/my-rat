#!/bin/sh
if [ -z "$JAVA_HOME" ] ; then
  echo "JAVA_HOME is not set"
  exit 1
fi
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
