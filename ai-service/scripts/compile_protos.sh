#!/bin/bash

echo "Compiling proto files..."

PROTO_DIR="protos"
OUTPUT_DIR="protos/generated"

# Clean old files
rm -rf $OUTPUT_DIR/*.py

mkdir -p $OUTPUT_DIR
touch $OUTPUT_DIR/__init__.py

# Question Service proto 컴파일
poetry run python -m grpc_tools.protoc \
    -I$PROTO_DIR \
    --python_out=$OUTPUT_DIR \
    --grpc_python_out=$OUTPUT_DIR \
    $PROTO_DIR/question_service.proto

# Quiz Service proto 컴파일
poetry run python -m grpc_tools.protoc \
    -I$PROTO_DIR \
    --python_out=$OUTPUT_DIR \
    --grpc_python_out=$OUTPUT_DIR \
    $PROTO_DIR/quiz_service.proto

echo "Fixing import paths..."
# Fix relative imports in generated files
if [ -f "$OUTPUT_DIR/question_service_pb2_grpc.py" ]; then
    sed -i 's/^import question_service_pb2/from . import question_service_pb2/g' $OUTPUT_DIR/question_service_pb2_grpc.py
fi

if [ -f "$OUTPUT_DIR/quiz_service_pb2_grpc.py" ]; then
    sed -i 's/^import quiz_service_pb2/from . import quiz_service_pb2/g' $OUTPUT_DIR/quiz_service_pb2_grpc.py
fi

echo "Proto compilation completed!"
echo "Generated files in: $OUTPUT_DIR"