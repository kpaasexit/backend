#!/bin/bash

set -e

echo "=== Starting AI Service ==="

# Download models from HuggingFace if not exists
if [ ! -f "/app/app/models/home_life_finetuned/final_model/model.onnx" ]; then
    echo "Models not found. Downloading from HuggingFace..."
    /app/scripts/download_models_from_hf.sh
else
    echo "Models already exist. Skipping download."
fi

# Start the application
echo "Starting uvicorn server..."
exec python -m uvicorn app.main:app --host 0.0.0.0 --port 8090 --workers "${WORKERS:-4}" ${RELOAD:+--reload}
