#!/bin/bash

set -e

echo "=== Downloading ONNX models from HuggingFace ==="

# Check if HUGGINGFACE_API_TOKEN is set
if [ -z "$HUGGINGFACE_API_TOKEN" ]; then
    echo "Warning: HUGGINGFACE_API_TOKEN not set. Trying to download without authentication..."
fi

# Base directory
BASE_DIR="/app/app/models"

# Function to download model
download_model() {
    local repo=$1
    local target_dir=$2
    local check_file=$3

    echo "Downloading $repo to $target_dir..."

    # Check if model file exists, not just directory
    if [ ! -f "$check_file" ]; then
        echo "Model file not found at $check_file, downloading..."
        mkdir -p "$target_dir"

        if [ -n "$HUGGINGFACE_API_TOKEN" ]; then
            huggingface-cli download "$repo" --local-dir "$target_dir" --token "$HUGGINGFACE_API_TOKEN"
        else
            huggingface-cli download "$repo" --local-dir "$target_dir"
        fi

        echo "✅ Downloaded: $repo"
    else
        echo "⏭️  Skipped (already exists): $check_file"
    fi
}

# Download models
download_model "MongsangGa/embeddings_onnx-all-minilm-l6-v2" "$BASE_DIR/embeddings_onnx/all-MiniLM-L6-v2" "$BASE_DIR/embeddings_onnx/all-MiniLM-L6-v2/model.onnx"
download_model "MongsangGa/embeddings_onnx-ko-sroberta" "$BASE_DIR/embeddings_onnx/ko-sroberta" "$BASE_DIR/embeddings_onnx/ko-sroberta/model.onnx"
download_model "MongsangGa/home_life_improved-onnx" "$BASE_DIR/home_life_finetuned/final_model" "$BASE_DIR/home_life_finetuned/final_model/model.onnx"

echo "=== All models downloaded successfully ==="
