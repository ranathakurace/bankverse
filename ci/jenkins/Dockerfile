FROM maven:3.9.9-eclipse-temurin-17

# Chrome and its matching driver support the Selenium regression in an Ubuntu agent.
RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium chromium-driver \
    && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium
ENV SE_AVOID_STATS=true
