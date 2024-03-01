FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jre-jammy AS builder

ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}

WORKDIR /app
ADD . .
RUN ./gradlew --no-daemon assemble

FROM eclipse-temurin:21-jre-jammy
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}


RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get -y install python3 && \
    apt-get -y install pip && \
    apt-get -y install python3.10-venv && \
    rm -rf /var/lib/apt/lists/*

ENV PYTHONFAULTHANDLER=1 \
    PYTHONHASHSEED=random \
    PYTHONUNBUFFERED=1

WORKDIR /app

ENV PIP_DEFAULT_TIMEOUT=100 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1 \
    POETRY_VERSION=1.4.2

# build-time OS dependencies
# RUN apk add --no-cache gcc musl-dev libffi-dev g++

# install Poetry
RUN pip install "poetry==$POETRY_VERSION"

# create virtual environment
RUN python3 -m venv /venv

# install Python dependencies in virtual environment
COPY pyproject.toml poetry.lock ./
RUN poetry export -f requirements.txt --output requirements.txt
# Remove unwanted Windows dependencies
RUN cat ./requirements.txt | sed -e :a -e '/\\$/N; s/\\\n//; ta' | sed 's/^pywin32==.*//' > requirements.txt
RUN /venv/bin/pip install -r requirements.txt

# build the app in virtual environment
COPY . .
RUN poetry build
RUN /venv/bin/pip install dist/*.whl

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN addgroup --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser --gid 2000

WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/build/libs/hmpps-person-record*.jar /app/app.jar
COPY --from=builder --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=builder --chown=appuser:appgroup /app/applicationinsights.json /app
COPY --from=builder --chown=appuser:appgroup /app/applicationinsights.dev.json /app

USER 2000

ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
