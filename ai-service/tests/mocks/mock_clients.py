"""Mock client implementations for testing."""

import asyncio
import numpy as np
from typing import List, Dict, Any, Optional, Union, AsyncGenerator, Callable
from unittest.mock import MagicMock, AsyncMock
from dataclasses import dataclass
import time


@dataclass
class MockQdrantPoint:
    """Mock Qdrant point for testing."""
    id: Union[int, str]
    payload: Dict[str, Any]
    vector: List[float]


@dataclass
class MockQdrantSearchResult:
    """Mock Qdrant search result for testing."""
    id: Union[int, str]
    payload: Dict[str, Any]
    score: float


class MockQdrantClient:
    """Mock Qdrant client for testing."""

    def __init__(self):
        self.collections = {}
        self.points_storage = {}  # collection_name -> {point_id: MockQdrantPoint}
        self._connected = True

    def get_collections(self):
        """Mock get collections."""
        mock_response = MagicMock()
        mock_response.collections = [
            MagicMock(name=name) for name in self.collections.keys()
        ]
        return mock_response

    def collection_exists(self, collection_name: str) -> bool:
        """Check if collection exists."""
        return collection_name in self.collections

    def create_collection(
        self,
        collection_name: str,
        vectors_config: Dict[str, Any],
        **kwargs
    ) -> bool:
        """Mock create collection."""
        self.collections[collection_name] = {
            "vectors_config": vectors_config,
            "created_at": time.time(),
            **kwargs
        }
        self.points_storage[collection_name] = {}
        return True

    def delete_collection(self, collection_name: str) -> bool:
        """Mock delete collection."""
        if collection_name in self.collections:
            del self.collections[collection_name]
            if collection_name in self.points_storage:
                del self.points_storage[collection_name]
            return True
        return False

    def upsert(
        self,
        collection_name: str,
        points: List[Dict[str, Any]],
        **kwargs
    ) -> MagicMock:
        """Mock upsert points."""
        if collection_name not in self.points_storage:
            self.points_storage[collection_name] = {}

        for point_data in points:
            point_id = point_data["id"]
            point = MockQdrantPoint(
                id=point_id,
                payload=point_data.get("payload", {}),
                vector=point_data["vector"]
            )
            self.points_storage[collection_name][point_id] = point

        # Return mock operation info
        result = MagicMock()
        result.operation_id = "mock_op_123"
        result.status = "completed"
        return result

    def search(
        self,
        collection_name: str,
        query_vector: List[float],
        limit: int = 10,
        score_threshold: Optional[float] = None,
        query_filter: Optional[Dict] = None,
        **kwargs
    ) -> List[MockQdrantSearchResult]:
        """Mock search points."""
        if collection_name not in self.points_storage:
            return []

        points = list(self.points_storage[collection_name].values())

        # Apply filter if provided
        if query_filter:
            points = self._apply_filter(points, query_filter)

        # Calculate mock similarity scores
        query_vector_np = np.array(query_vector)
        results = []

        for point in points:
            point_vector_np = np.array(point.vector)

            # Calculate cosine similarity
            if len(query_vector_np) == len(point_vector_np):
                similarity = np.dot(query_vector_np, point_vector_np) / (
                    np.linalg.norm(query_vector_np) * np.linalg.norm(point_vector_np)
                )
            else:
                # Different dimensions - use random similarity for testing
                similarity = np.random.rand() * 0.5 + 0.3

            score = max(0.0, min(1.0, similarity))

            if score_threshold is None or score >= score_threshold:
                results.append(MockQdrantSearchResult(
                    id=point.id,
                    payload=point.payload,
                    score=score
                ))

        # Sort by score (descending) and limit
        results.sort(key=lambda x: x.score, reverse=True)
        return results[:limit]

    def scroll(
        self,
        collection_name: str,
        scroll_filter: Optional[Dict] = None,
        limit: int = 10,
        offset: Optional[str] = None,
        **kwargs
    ) -> tuple:
        """Mock scroll through points."""
        if collection_name not in self.points_storage:
            return [], None

        points = list(self.points_storage[collection_name].values())

        # Apply filter if provided
        if scroll_filter:
            points = self._apply_filter(points, scroll_filter)

        # Simple pagination simulation
        start_idx = int(offset) if offset and offset.isdigit() else 0
        end_idx = start_idx + limit

        page_points = points[start_idx:end_idx]
        next_offset = str(end_idx) if end_idx < len(points) else None

        return page_points, next_offset

    def delete(
        self,
        collection_name: str,
        points_selector: Union[List[Union[int, str]], Dict],
        **kwargs
    ) -> MagicMock:
        """Mock delete points."""
        if collection_name not in self.points_storage:
            return MagicMock()

        if isinstance(points_selector, list):
            # Delete by IDs
            for point_id in points_selector:
                self.points_storage[collection_name].pop(str(point_id), None)
        elif isinstance(points_selector, dict):
            # Delete by filter
            points_to_delete = []
            for point_id, point in self.points_storage[collection_name].items():
                if self._matches_filter(point, points_selector):
                    points_to_delete.append(point_id)

            for point_id in points_to_delete:
                del self.points_storage[collection_name][point_id]

        result = MagicMock()
        result.operation_id = "mock_delete_123"
        result.status = "completed"
        return result

    def get_collection_info(self, collection_name: str) -> MagicMock:
        """Mock get collection info."""
        info = MagicMock()
        if collection_name in self.collections:
            info.config = self.collections[collection_name]
            info.status = "green"
            info.points_count = len(self.points_storage.get(collection_name, {}))
        else:
            info.status = "not_found"
            info.points_count = 0

        return info

    def _apply_filter(
        self,
        points: List[MockQdrantPoint],
        filter_dict: Dict
    ) -> List[MockQdrantPoint]:
        """Apply filter to points."""
        filtered_points = []
        for point in points:
            if self._matches_filter(point, filter_dict):
                filtered_points.append(point)
        return filtered_points

    def _matches_filter(
        self,
        point: MockQdrantPoint,
        filter_dict: Dict
    ) -> bool:
        """Check if point matches filter."""
        # Simplified filter matching for testing
        if "must" in filter_dict:
            for condition in filter_dict["must"]:
                if not self._matches_condition(point, condition):
                    return False

        if "should" in filter_dict:
            if not any(self._matches_condition(point, condition)
                      for condition in filter_dict["should"]):
                return False

        return True

    def _matches_condition(
        self,
        point: MockQdrantPoint,
        condition: Dict
    ) -> bool:
        """Check if point matches a single condition."""
        # Very basic condition matching for testing
        if "key" in condition and "match" in condition:
            field_value = point.payload.get(condition["key"])
            return field_value == condition["match"]["value"]

        return True

    def close(self):
        """Mock close connection."""
        self._connected = False


class MockRedisClient:
    """Mock Redis client for testing."""

    def __init__(self):
        self.data = {}
        self._connected = True

    def get(self, key: str) -> Optional[bytes]:
        """Mock get value."""
        value = self.data.get(key)
        return value.encode() if value else None

    def set(
        self,
        key: str,
        value: Union[str, bytes],
        ex: Optional[int] = None,
        **kwargs
    ) -> bool:
        """Mock set value."""
        if isinstance(value, bytes):
            value = value.decode()

        self.data[key] = value

        # Simulate expiration (simplified)
        if ex:
            import threading
            def expire():
                time.sleep(ex)
                self.data.pop(key, None)

            threading.Timer(ex, expire).start()

        return True

    def delete(self, *keys: str) -> int:
        """Mock delete keys."""
        deleted = 0
        for key in keys:
            if key in self.data:
                del self.data[key]
                deleted += 1
        return deleted

    def exists(self, *keys: str) -> int:
        """Mock check if keys exist."""
        return sum(1 for key in keys if key in self.data)

    def ping(self) -> bool:
        """Mock ping."""
        return self._connected

    def flushdb(self) -> bool:
        """Mock flush database."""
        self.data.clear()
        return True

    def keys(self, pattern: str = "*") -> List[str]:
        """Mock get keys by pattern."""
        if pattern == "*":
            return list(self.data.keys())

        # Simple pattern matching for testing
        import fnmatch
        return [key for key in self.data.keys() if fnmatch.fnmatch(key, pattern)]

    def hset(
        self,
        name: str,
        key: Optional[str] = None,
        value: Optional[str] = None,
        mapping: Optional[Dict] = None
    ) -> int:
        """Mock hash set."""
        if name not in self.data:
            self.data[name] = {}

        if not isinstance(self.data[name], dict):
            self.data[name] = {}

        if mapping:
            self.data[name].update(mapping)
            return len(mapping)
        elif key is not None and value is not None:
            self.data[name][key] = value
            return 1

        return 0

    def hget(self, name: str, key: str) -> Optional[str]:
        """Mock hash get."""
        if name in self.data and isinstance(self.data[name], dict):
            return self.data[name].get(key)
        return None

    def hgetall(self, name: str) -> Dict[str, str]:
        """Mock hash get all."""
        if name in self.data and isinstance(self.data[name], dict):
            return self.data[name].copy()
        return {}

    def close(self):
        """Mock close connection."""
        self._connected = False


class MockOpenAIClient:
    """Mock OpenAI client for testing."""

    def __init__(self):
        self.embeddings = MockEmbeddingsAPI()
        self.chat = MockChatAPI()


class MockEmbeddingsAPI:
    """Mock OpenAI embeddings API."""

    def create(
        self,
        input: Union[str, List[str]],
        model: str = "text-embedding-3-small",
        **kwargs
    ) -> MagicMock:
        """Mock create embeddings."""
        if isinstance(input, str):
            input_list = [input]
        else:
            input_list = input

        # Generate deterministic embeddings based on text hash
        embeddings_data = []
        for text in input_list:
            text_hash = hash(text) % (2**31)
            np.random.seed(text_hash)

            # Different models have different dimensions
            if "3-large" in model:
                dimension = 3072
            elif "3-small" in model:
                dimension = 1536
            elif "ada-002" in model:
                dimension = 1536
            else:
                dimension = 1536

            embedding = np.random.rand(dimension).tolist()
            embeddings_data.append(MagicMock(embedding=embedding))

        # Create mock response
        response = MagicMock()
        response.data = embeddings_data
        response.model = model
        response.usage = MagicMock(
            prompt_tokens=sum(len(text.split()) for text in input_list),
            total_tokens=sum(len(text.split()) for text in input_list)
        )

        return response


class MockChatAPI:
    """Mock OpenAI chat API."""

    def __init__(self):
        self.completions = MockChatCompletions()


class MockChatCompletions:
    """Mock OpenAI chat completions API."""

    def create(
        self,
        model: str,
        messages: List[Dict[str, str]],
        temperature: float = 0.7,
        max_tokens: Optional[int] = None,
        **kwargs
    ) -> MagicMock:
        """Mock create chat completion."""

        # Generate response based on the last user message
        last_message = ""
        for msg in reversed(messages):
            if msg.get("role") == "user":
                last_message = msg.get("content", "")
                break

        # Generate context-appropriate response
        response_content = self._generate_mock_response(last_message)

        # Create mock response
        response = MagicMock()

        mock_choice = MagicMock()
        mock_message = MagicMock()
        mock_message.content = response_content
        mock_message.role = "assistant"
        mock_choice.message = mock_message
        mock_choice.finish_reason = "stop"

        response.choices = [mock_choice]
        response.model = model
        response.usage = MagicMock(
            prompt_tokens=sum(len(msg.get("content", "").split()) for msg in messages),
            completion_tokens=len(response_content.split()),
            total_tokens=sum(len(msg.get("content", "").split()) for msg in messages) + len(response_content.split())
        )

        return response

    def _generate_mock_response(self, user_message: str) -> str:
        """Generate contextually appropriate mock response."""
        user_lower = user_message.lower()

        # Category-specific responses for testing
        if any(word in user_lower for word in ["김치", "요리", "음식", "레시피"]):
            return "김치찌개를 맛있게 끓이려면 먼저 김치를 기름에 볶아주세요. 그 다음 물을 넣고 돼지고기를 추가한 후 끓여주시면 됩니다."

        elif any(word in user_lower for word in ["청소", "세탁", "얼룩"]):
            return "얼룩 제거에는 베이킹소다와 식초를 섞은 용액이 효과적입니다. 얼룩에 발라두고 30분 후 찬물로 헹구세요."

        elif any(word in user_lower for word in ["수리", "고장", "diy"]):
            return "형광등 교체는 전원을 차단한 후 안전하게 진행하세요. 형광등 양쪽을 90도 돌려서 제거하고 새 것으로 교체하면 됩니다."

        elif any(word in user_lower for word in ["계약", "전세", "대출"]):
            return "전세 계약 시에는 등기부등본을 반드시 확인하고, 보증금 대비 매매가격 비율을 체크해야 합니다. 전세보증보험 가입도 권장합니다."

        elif any(word in user_lower for word in ["강아지", "고양이", "반려동물"]):
            return "강아지 배변훈련은 일정한 시간에 같은 장소에서 시키는 것이 중요합니다. 성공했을 때 충분한 보상을 주세요."

        elif any(word in user_lower for word in ["인테리어", "이사", "꾸미기"]):
            return "원룸 인테리어는 벽면 활용이 핵심입니다. 수직 공간을 최대한 활용하고 밝은 색상으로 공간감을 늘리세요."

        elif any(word in user_lower for word in ["건강", "환경", "미세먼지"]):
            return "미세먼지가 심한 날에는 외출을 자제하고, 실내 공기청정기를 가동하세요. 마스크 착용도 필수입니다."

        elif any(word in user_lower for word in ["스마트홈", "가전", "에어컨"]):
            return "에어컨 필터는 월 1회 이상 청소해주세요. 필터를 분리한 후 찬물에 중성세제로 세척하고 완전히 말려서 장착하면 됩니다."

        else:
            return "안녕하세요! 생활 관련 질문에 대해 도움을 드릴 수 있습니다. 구체적인 상황을 알려주시면 더 정확한 답변을 드리겠습니다."