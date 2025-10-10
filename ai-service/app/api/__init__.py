"""FastAPI REST API 라우터"""

from .search import router as search_router

__all__ = ["search_router"]
