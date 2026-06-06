#!/bin/bash
# Download gradle-wrapper.jar for Gradle 8.2

GRADLE_VERSION="8.2.0"
WRAPPER_JAR_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_JAR_PATH="gradle/wrapper/gradle-wrapper.jar"

echo "Downloading gradle-wrapper.jar for Gradle ${GRADLE_VERSION}..."
curl -L "$WRAPPER_JAR_URL" -o "$WRAPPER_JAR_PATH"

if [ -f "$WRAPPER_JAR_PATH" ]; then
    echo "✅ Successfully downloaded gradle-wrapper.jar"
    echo "File size: $(du -h $WRAPPER_JAR_PATH | cut -f1)"
else
    echo "❌ Failed to download gradle-wrapper.jar"
    exit 1
fi
