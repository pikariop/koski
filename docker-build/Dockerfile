FROM adoptopenjdk/openjdk11:alpine-slim@sha256:7882f971d03f637ffb4d39efca2a46c7c60af253327b17fd968961a4abb69857

ARG KOSKI_VERSION
ARG PROMETHEUS_JMX_EXPORTER_VERSION="0.20.0"
ARG PROMETHEUS_JMX_EXPORTER_JAR_HASH="8b67ed40485d475648c36d11d21464b484406f85faf912b3363f71a6a7861320"

# Install:
# * tzdata for timezones
# * fonts (see: https://github.com/docker-library/openjdk/issues/73#issuecomment-207816707)
RUN apk add --no-cache tzdata ttf-dejavu

# Set timezone
RUN cp /usr/share/zoneinfo/Europe/Helsinki /etc/localtime && apk del tzdata

# JVM reads timezone from this file instead:
RUN echo 'Europe/Helsinki' > /etc/timezone

# Install Prometheus JMX exporter
RUN wget -q https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${PROMETHEUS_JMX_EXPORTER_VERSION}/jmx_prometheus_javaagent-${PROMETHEUS_JMX_EXPORTER_VERSION}.jar \
    -O /usr/local/bin/jmx_prometheus_javaagent.jar && \
    echo "$PROMETHEUS_JMX_EXPORTER_JAR_HASH  /usr/local/bin/jmx_prometheus_javaagent.jar" | sha256sum -c
COPY docker-build/jmx_exporter_config.yml /etc

COPY docker-build/run.sh /usr/local/bin
RUN chmod +x /usr/local/bin/run.sh

RUN addgroup -S koski -g 10001 && adduser -u 10000 -S -G koski koski

# Defang bins
RUN find / -xdev -perm +6000 -type f -exec chmod a-s {} \; || true

USER koski

RUN mkdir -p /home/koski/heapdumps

COPY target/dist/target/koski-${KOSKI_VERSION}.war /home/koski
RUN unzip -d /home/koski /home/koski/koski-${KOSKI_VERSION}.war && rm /home/koski/koski-${KOSKI_VERSION}.war

COPY docker-build/log4j2.xml /home/koski

# Koski app
EXPOSE 8080
# Prometheus JMX exporter
EXPOSE 9101
# JMX
EXPOSE 5555

VOLUME /tmp

ENTRYPOINT /usr/local/bin/run.sh
