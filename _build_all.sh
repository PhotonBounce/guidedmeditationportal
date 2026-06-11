export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew.bat assembleDebug bundleRelease --no-daemon 2>&1 | grep -E "^e:|error:|BUILD SUCCESS|BUILD FAILED"
