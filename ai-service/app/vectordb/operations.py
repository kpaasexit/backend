"""Vector database operations - backward compatibility layer."""

from .question_operations import QuestionVectorOperations
from .interfaces import VectorOperationsInterface

# Re-export for backward compatibility
VectorOperations = QuestionVectorOperations


# Global instance
_vector_operations = None


def get_vector_operations():
    """Get or create global vector operations instance."""
    global _vector_operations
    if _vector_operations is None:
        _vector_operations = VectorOperations()
    return _vector_operations