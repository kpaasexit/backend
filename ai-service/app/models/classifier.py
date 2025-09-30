import os
import threading
from typing import List, Optional, Tuple
import torch
from torch.nn import functional as F

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
        self.device = torch.device("cpu")
        self.model = None
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
                from transformers import RobertaForSequenceClassification, BertTokenizer

                # Get the base directory for models
                # In Docker container: /app/app/models/
                # In local development: /home/ubuntu/backend/ai-service/app/models/
                current_dir = os.path.dirname(os.path.abspath(__file__))

                finetuned_path = os.path.join(current_dir, "home_life_finetuned")
                base_path = os.path.join(current_dir, "home_life_classifier")

                # Check which path exists and has required files
                if os.path.exists(os.path.join(finetuned_path, 'config.json')):
                    model_path = finetuned_path
                elif os.path.exists(os.path.join(base_path, 'config.json')):
                    model_path = base_path
                else:
                    # Debug: print available files
                    print(f"Current directory: {current_dir}")
                    print(f"Files in current directory: {os.listdir(current_dir)}")
                    print(f"Checking finetuned_path: {finetuned_path}, exists: {os.path.exists(finetuned_path)}")
                    print(f"Checking base_path: {base_path}, exists: {os.path.exists(base_path)}")
                    raise FileNotFoundError(f"Model files not found. Searched in: {finetuned_path} and {base_path}")

                # Explicitly set to use local files only
                os.environ['TRANSFORMERS_OFFLINE'] = '1'

                # Load tokenizer using BertTokenizer (as specified in config)
                self.tokenizer = BertTokenizer.from_pretrained(
                    model_path,
                    local_files_only=True
                )

                torch.set_default_dtype(torch.float32)

                # Load the model
                self.model = RobertaForSequenceClassification.from_pretrained(
                    model_path,
                    num_labels=8,
                    local_files_only=True
                )

                self.model = self.model.float()

                self.model = self.model.to(self.device)
                self.model.eval()

                self._initialized = True

            except Exception as e:
                raise ClassificationError(text="", reason=f"Model initialization failed: {e}")

    def classify(self, text: str) -> Tuple[str, float]:
        if not text or not text.strip():
            raise ClassificationError(text=text, reason="Empty or invalid text")

        if not self._initialized:
            self.initialize()

        try:
            inputs = self.tokenizer(
                text,
                truncation=True,
                padding=True,
                max_length=128,
                return_tensors="pt"
            )

            inputs = {k: v.to(self.device) for k, v in inputs.items()}

            with torch.no_grad():
                outputs = self.model(**inputs)
                logits = outputs.logits

                probabilities = F.softmax(logits, dim=-1)

                predicted_idx = torch.argmax(probabilities, dim=-1).item()
                confidence = probabilities[0, predicted_idx].item()

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
            inputs = self.tokenizer(
                texts,
                truncation=True,
                padding=True,
                max_length=128,
                return_tensors="pt"
            )

            inputs = {k: v.to(self.device) for k, v in inputs.items()}

            with torch.no_grad():
                outputs = self.model(**inputs)
                logits = outputs.logits

                probabilities = F.softmax(logits, dim=-1)

                predicted_indices = torch.argmax(probabilities, dim=-1)
                confidences = torch.max(probabilities, dim=-1).values

            results = []
            for idx, conf in zip(predicted_indices.tolist(), confidences.tolist()):
                category = HOME_LIFE_CATEGORIES[idx]
                results.append((category, float(conf)))

            return results

        except Exception as e:
            return [self.classify(text) for text in texts]

    def get_category_id(self, category_name: str) -> Optional[int]:
        try:
            return HOME_LIFE_CATEGORIES.index(category_name)
        except ValueError:
            return None


_classifier: Optional[HomeLifeClassifier] = None


def get_classifier() -> HomeLifeClassifier:
    global _classifier
    if _classifier is None:
        _classifier = HomeLifeClassifier()
    return _classifier


get_home_life_classifier = get_classifier