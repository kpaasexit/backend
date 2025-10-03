import os
import threading
from typing import List, Optional, Tuple
import numpy as np

from app.config import get_settings
from app.core.exceptions import ClassificationError


HOME_LIFE_CATEGORIES = [
    "생활경제/계약",
    "생활수리/DIY",
    "스마트홈/가전",
    "요리/식품관리",
    "육아/반려동물",
    "이사/인테리어",
    "청소/세탁",
    "환경/건강"
]


class HomeLifeClassifier:
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if hasattr(self, '_initialized_flag'):
            return

        self.settings = get_settings()
        self.session = None
        self.tokenizer = None
        self._initialized = False
        self._init_lock = threading.Lock()
        self._initialized_flag = True

    def initialize(self):
        if self._initialized:
            return

        with self._init_lock:
            if self._initialized:
                return

            try:
                import onnxruntime as ort
                from transformers import BertTokenizer

                # Get the base directory for models
                # In Docker container: /app/app/models/
                # In local development: /home/ubuntu/backend/ai-service/app/models/
                current_dir = os.path.dirname(os.path.abspath(__file__))

                # Fine-tuned model path (with final_model subdirectory)
                finetuned_path = os.path.join(current_dir, "home_life_finetuned", "final_model")
                onnx_path = os.path.join(finetuned_path, "model.onnx")

                # Check if ONNX model exists
                if not os.path.exists(onnx_path):
                    # Debug: print available files
                    print(f"Current directory: {current_dir}")
                    print(f"Files in current directory: {os.listdir(current_dir)}")
                    print(f"Checking onnx_path: {onnx_path}, exists: {os.path.exists(onnx_path)}")
                    raise FileNotFoundError(f"ONNX model not found at: {onnx_path}")

                # Load tokenizer
                self.tokenizer = BertTokenizer.from_pretrained(
                    finetuned_path,
                    local_files_only=True
                )

                # Create ONNX Runtime session
                sess_options = ort.SessionOptions()
                sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
                sess_options.intra_op_num_threads = 4

                self.session = ort.InferenceSession(
                    onnx_path,
                    sess_options=sess_options,
                    providers=['CPUExecutionProvider']
                )

                self._initialized = True
                print(f"✓ ONNX model loaded from: {onnx_path}")

            except Exception as e:
                raise ClassificationError(text="", reason=f"Model initialization failed: {e}")

    def classify(self, text: str) -> Tuple[str, float]:
        if not text or not text.strip():
            raise ClassificationError(text=text, reason="Empty or invalid text")

        if not self._initialized:
            self.initialize()

        try:
            # Tokenize input
            inputs = self.tokenizer(
                text,
                truncation=True,
                padding="max_length",
                max_length=128,
                return_tensors="np"
            )

            # Run inference
            ort_inputs = {
                "input_ids": inputs["input_ids"].astype(np.int64),
                "attention_mask": inputs["attention_mask"].astype(np.int64)
            }
            logits = self.session.run(None, ort_inputs)[0]

            # Apply softmax to get probabilities
            logits_exp = np.exp(logits - np.max(logits, axis=-1, keepdims=True))
            probabilities = logits_exp / np.sum(logits_exp, axis=-1, keepdims=True)

            # Get prediction
            predicted_idx = np.argmax(probabilities, axis=-1)[0]
            confidence = probabilities[0, predicted_idx]

            category = HOME_LIFE_CATEGORIES[predicted_idx]

            return category, float(confidence)

        except Exception as e:
            raise ClassificationError(text=text, reason=str(e))

    def batch_classify(self, texts: List[str]) -> List[Tuple[str, float]]:
        if not self._initialized:
            self.initialize()

        if not texts:
            return []

        try:
            # Tokenize all inputs
            inputs = self.tokenizer(
                texts,
                truncation=True,
                padding="max_length",
                max_length=128,
                return_tensors="np"
            )

            # Run inference
            ort_inputs = {
                "input_ids": inputs["input_ids"].astype(np.int64),
                "attention_mask": inputs["attention_mask"].astype(np.int64)
            }
            logits = self.session.run(None, ort_inputs)[0]

            # Apply softmax to get probabilities
            logits_exp = np.exp(logits - np.max(logits, axis=-1, keepdims=True))
            probabilities = logits_exp / np.sum(logits_exp, axis=-1, keepdims=True)

            # Get predictions
            predicted_indices = np.argmax(probabilities, axis=-1)
            confidences = np.max(probabilities, axis=-1)

            results = []
            for idx, conf in zip(predicted_indices, confidences):
                category = HOME_LIFE_CATEGORIES[idx]
                results.append((category, float(conf)))

            return results

        except Exception as e:
            # Fallback to single classification
            return [self.classify(text) for text in texts]

    def get_category_id(self, category_name: str) -> Optional[int]:
        """
        카테고리 이름을 ID로 변환 (API용 1-based)
        학습 시에는 0-based를 사용하지만, API에서는 1-based ID를 사용
        """
        try:
            return HOME_LIFE_CATEGORIES.index(category_name) + 1
        except ValueError:
            return None


_classifier: Optional[HomeLifeClassifier] = None


def get_classifier() -> HomeLifeClassifier:
    global _classifier
    if _classifier is None:
        _classifier = HomeLifeClassifier()
    return _classifier


get_home_life_classifier = get_classifier
