"""Qdrant vector database client."""

import os
from typing import Dict, Optional, Any
import asyncio

from qdrant_client import QdrantClient as QdrantClientBase
from qdrant_client.models import Distance, VectorParams
from qdrant_client.http import models

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.core.exceptions import VectorDBError


logger = LoggerSetup.get_logger(__name__)


class QdrantClient:
    """
    Qdrant vector database client wrapper.

    Features:
    - Connection pool management
    - Automatic reconnection
    - Health checks
    - Async operations support
    """

    def __init__(self):
        self.settings = get_settings()
        self._client: Optional[QdrantClientBase] = None
        self._is_connected = False
        self._connect_lock = asyncio.Lock()

    def connect(self) -> None:
        """Establish connection to Qdrant."""
        if self._is_connected:
            return

        try:
            # Initialize client
            if os.environ.get("TESTING", "").lower() == "true":
                self._client = QdrantClientBase(":memory:")
            else:
                self._client = QdrantClientBase(
                    url=f"http://{self.settings.qdrant.qdrant_host}:{self.settings.qdrant.qdrant_port}",
                    api_key=self.settings.qdrant.qdrant_api_key,
                    timeout=30,
                    prefer_grpc=False  # Use HTTP for better compatibility
                )

            # Test connection
            self._client.get_collections()
            self._is_connected = True
            logger.info(f"Connected to Qdrant at {self.settings.qdrant.qdrant_host}:{self.settings.qdrant.qdrant_port}")

        except Exception as e:
            self._is_connected = False
            logger.error(f"Failed to connect to Qdrant: {e}")
            raise VectorDBError(operation="connect", reason=str(e))

    async def aconnect(self) -> None:
        """Async connection to Qdrant."""
        async with self._connect_lock:
            if self._is_connected:
                return

            await asyncio.get_event_loop().run_in_executor(None, self.connect)

    def is_alive(self) -> bool:
        """
        Check if Qdrant connection is alive.

        Returns:
            True if connected and responsive
        """
        try:
            if not self._is_connected:
                self.connect()

            # Try to get collections as a health check
            self._client.get_collections()
            return True
        except Exception:
            return False

    def health_check(self) -> Dict[str, Any]:
        """
        Check Qdrant health status.

        Returns:
            Health status information
        """
        if not self._is_connected:
            self.connect()

        try:
            # Get cluster info
            info = self._client.get_collections()

            return {
                "status": "healthy",
                "connected": True,
                "collections_count": len(info.collections) if info else 0,
                "host": self.settings.qdrant.qdrant_host,
                "port": self.settings.qdrant.qdrant_port
            }

        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return {
                "status": "unhealthy",
                "connected": False,
                "error": str(e),
                "host": self.settings.qdrant.qdrant_host,
                "port": self.settings.qdrant.qdrant_port
            }

    def get_client(self) -> QdrantClientBase:
        """
        Get Qdrant client instance.

        Returns:
            Qdrant client

        Raises:
            VectorDBError: If not connected
        """
        if not self._is_connected or not self._client:
            self.connect()

        if not self._client:
            raise VectorDBError(
                operation="get_client",
                reason="Client not initialized"
            )

        return self._client

    def close(self) -> None:
        """Close Qdrant connection."""
        if self._client:
            try:
                self._client.close()
                logger.info("Qdrant connection closed")
            except Exception as e:
                logger.error(f"Error closing Qdrant connection: {e}")
            finally:
                self._client = None
                self._is_connected = False

    async def aclose(self) -> None:
        """Async close Qdrant connection."""
        await asyncio.get_event_loop().run_in_executor(None, self.close)

    def create_collection(
        self,
        collection_name: str,
        vector_size: int,
        distance: Distance = Distance.COSINE,
        on_disk_payload: bool = True,
        **kwargs
    ) -> bool:
        """
        Create a new collection.

        Args:
            collection_name: Name of the collection
            vector_size: Dimension of vectors
            distance: Distance metric
            on_disk_payload: Store payload on disk for large collections
            **kwargs: Additional collection parameters

        Returns:
            True if successful
        """
        client = self.get_client()

        try:
            client.create_collection(
                collection_name=collection_name,
                vectors_config=VectorParams(
                    size=vector_size,
                    distance=distance,
                    on_disk=on_disk_payload
                ),
                **kwargs
            )
            logger.info(f"Created collection: {collection_name}")
            return True

        except Exception as e:
            if "already exists" in str(e).lower():
                logger.info(f"Collection {collection_name} already exists")
                return True
            logger.error(f"Failed to create collection {collection_name}: {e}")
            raise VectorDBError(operation="create_collection", reason=str(e))

    def delete_collection(self, collection_name: str) -> bool:
        """
        Delete a collection.

        Args:
            collection_name: Name of the collection

        Returns:
            True if successful
        """
        client = self.get_client()

        try:
            client.delete_collection(collection_name=collection_name)
            logger.info(f"Deleted collection: {collection_name}")
            return True

        except Exception as e:
            logger.error(f"Failed to delete collection {collection_name}: {e}")
            raise VectorDBError(operation="delete_collection", reason=str(e))

    def collection_exists(self, collection_name: str) -> bool:
        """
        Check if collection exists.

        Args:
            collection_name: Name of the collection

        Returns:
            True if exists
        """
        client = self.get_client()

        try:
            collections = client.get_collections()
            return any(c.name == collection_name for c in collections.collections)

        except Exception as e:
            logger.error(f"Failed to check collection existence: {e}")
            return False

    def get_collection_info(self, collection_name: str) -> Optional[Dict[str, Any]]:
        """
        Get collection information.

        Args:
            collection_name: Name of the collection

        Returns:
            Collection info or None if not found
        """
        client = self.get_client()

        try:
            info = client.get_collection(collection_name=collection_name)

            return {
                "name": collection_name,
                "vector_size": info.config.params.vectors.size,
                "distance": info.config.params.vectors.distance,
                "points_count": info.points_count,
                "indexed_vectors_count": info.indexed_vectors_count,
                "status": info.status
            }

        except Exception as e:
            logger.error(f"Failed to get collection info: {e}")
            return None

    def optimize_collection(
        self,
        collection_name: str,
        wait: bool = False
    ) -> bool:
        """
        Optimize collection for better performance.

        Args:
            collection_name: Name of the collection
            wait: Whether to wait for optimization to complete

        Returns:
            True if successful
        """
        client = self.get_client()

        try:
            client.update_collection(
                collection_name=collection_name,
                optimizer_config=models.OptimizersConfigDiff(
                    indexing_threshold=10000,
                    memmap_threshold=100000,
                    flush_interval_sec=5
                )
            )

            if wait:
                # Wait for indexing to complete
                import time
                max_wait = 60  # seconds
                start_time = time.time()

                while time.time() - start_time < max_wait:
                    info = self.get_collection_info(collection_name)
                    if info and info["status"] == "green":
                        break
                    time.sleep(1)

            logger.info(f"Optimized collection: {collection_name}")
            return True

        except Exception as e:
            logger.error(f"Failed to optimize collection {collection_name}: {e}")
            return False

    def __enter__(self):
        """Context manager entry."""
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()

    async def __aenter__(self):
        """Async context manager entry."""
        await self.aconnect()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        await self.aclose()


# Global client instance
_qdrant_client: Optional[QdrantClient] = None


def get_qdrant_client() -> QdrantClient:
    """Get or create global Qdrant client instance."""
    global _qdrant_client
    if _qdrant_client is None:
        _qdrant_client = QdrantClient()
        _qdrant_client.connect()
    return _qdrant_client