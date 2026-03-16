FROM node:20-bookworm-slim

ENV NEXT_TELEMETRY_DISABLED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    GAT_PYTHON_BIN=python3 \
    GAT_PYTHON_ARGS= \
    HOSTNAME=0.0.0.0 \
    PORT=3000

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip python3-venv libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt ./
RUN python3 -m pip install --break-system-packages --upgrade pip setuptools wheel \
    && python3 -m pip install --break-system-packages -r requirements.txt

COPY frontend/package*.json /app/frontend/
WORKDIR /app/frontend
RUN npm ci

WORKDIR /app
COPY . .
RUN chmod +x /app/docker/entrypoint.sh /app/frontend/scripts/next-with-python-env.sh

WORKDIR /app/frontend
RUN npm run build

EXPOSE 3000

ENTRYPOINT ["/app/docker/entrypoint.sh"]
