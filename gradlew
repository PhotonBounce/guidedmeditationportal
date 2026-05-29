#!/usr/bin/env sh
# Gradle wrapper start-up script for UN*X
# (Linux/macOS — Windows uses gradlew.bat)

PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Bootstrap wrapper jar
if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Downloading gradle-wrapper.jar..."
    if command -v curl >/dev/null 2>&1; then
        curl -sSL -o "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
          "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
          "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
    else
        echo "curl or wget required to bootstrap gradle wrapper"; exit 1
    fi
fi

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi
if ! command -v "$JAVACMD" >/dev/null 2>&1 ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' in PATH."
    exit 1
fi

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$0" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
