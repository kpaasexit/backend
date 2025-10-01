"""Models module."""

from .manager import ModelManager, get_model_manager
from .classifier import HomeLifeClassifier as Classifier, get_classifier
from .embedder import TextEmbedder as Embedder, get_embedder
from .dimension_reducer import DimensionReducer, get_dimension_reducer
from .language import LanguageDetector, get_dominant_language, get_language_weights

# Memory management components
from .memory import MemoryManager
from .cache import ModelCache
from .loaders import ModelLoader

__all__ = [
    "ModelManager",
    "get_model_manager",
    "Classifier",
    "get_classifier",
    "Embedder",
    "get_embedder",
    "DimensionReducer",
    "get_dimension_reducer",
    "LanguageDetector",
    "get_dominant_language",
    "get_language_weights",
    "MemoryManager",
    "ModelCache",
    "ModelLoader"
]