FROM python:3.12-slim as builder

RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get update --fix-missing && \
    apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir poetry==1.7.1

WORKDIR /app

COPY pyproject.toml poetry.lock ./

RUN poetry config virtualenvs.create false \
    && poetry install --no-dev --no-interaction --no-ansi

FROM python:3.12-slim

RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get update --fix-missing && \
    apt-get install -y --no-install-recommends \
    curl \
    && apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

COPY app ./app
COPY protos ./protos
COPY scripts ./scripts

RUN chmod +x scripts/*.sh && \
    pip install grpcio-tools && \
    mkdir -p protos/generated && \
    python -m grpc_tools.protoc \
    -Iprotos \
    --python_out=protos/generated \
    --grpc_python_out=protos/generated \
    protos/question_service.proto && \
    python -m grpc_tools.protoc \
    -Iprotos \
    --python_out=protos/generated \
    --grpc_python_out=protos/generated \
    protos/quiz_service.proto && \
    sed -i 's/^import question_service_pb2/from . import question_service_pb2/g' protos/generated/question_service_pb2_grpc.py && \
    sed -i 's/^import quiz_service_pb2/from . import quiz_service_pb2/g' protos/generated/quiz_service_pb2_grpc.py && \
    touch protos/generated/__init__.py

# Create appuser and prepare directories with correct permissions
RUN useradd -m -u 1000 appuser && \
    mkdir -p /app/model_cache /app/logs && \
    chown -R appuser:appuser /app && \
    chmod 755 /app/model_cache /app/logs

USER appuser

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8090/health || exit 1

EXPOSE 8090 50051

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    WORKERS=4 \
    LOG_LEVEL=info

CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090", "--workers", "4", "--reload"]