# Reproducible Android CI image for local and pipeline builds.
# Usage: docker compose run --rm ci
FROM eclipse-temurin:22.0.2_9-jdk-jammy AS sdk

ARG ANDROID_SDK_TOOLS_VERSION=13114758
ARG ANDROID_COMPILE_SDK=36
ARG ANDROID_BUILD_TOOLS=36.0.0
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    DEBIAN_FRONTEND=noninteractive
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl git unzip zip

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
 && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_TOOLS_VERSION}_latest.zip" -o /tmp/cmdline-tools.zip \
 && unzip -q /tmp/cmdline-tools.zip -d /tmp \
 && mv /tmp/cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest" \
 && rm /tmp/cmdline-tools.zip

RUN yes | sdkmanager --licenses >/dev/null \
 && sdkmanager --install \
      "platform-tools" \
      "platforms;android-${ANDROID_COMPILE_SDK}" \
      "build-tools;${ANDROID_BUILD_TOOLS}"

FROM eclipse-temurin:22.0.2_9-jdk-jammy AS ci

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/home/tunr/.gradle
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN useradd --create-home --uid 1000 --shell /bin/bash tunr
COPY --from=sdk --chown=tunr:tunr /opt/android-sdk /opt/android-sdk

WORKDIR /workspace
USER tunr

CMD ["./gradlew", "--no-daemon", "testDebugUnitTest", "assembleDebug"]
