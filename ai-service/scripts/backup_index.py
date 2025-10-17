"""
Elasticsearch 인덱스 백업 스크립트

인덱싱된 데이터를 JSON 파일로 백업합니다.
"""

import asyncio
import os
import json
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Any

from elasticsearch import AsyncElasticsearch

import logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


INDEX_NAME = "naver_kin_autocomplete"
BACKUP_DIR = Path(__file__).parent / "backups"


async def backup_index(es: AsyncElasticsearch, index_name: str, output_file: Path):
    """인덱스의 모든 데이터를 JSON 파일로 백업"""

    logger.info(f"인덱스 '{index_name}' 백업 시작...")

    # 백업 디렉토리 생성
    output_file.parent.mkdir(parents=True, exist_ok=True)

    # 전체 문서 수 확인
    count_response = await es.count(index=index_name)
    total_docs = count_response['count']
    logger.info(f"총 {total_docs:,}개 문서를 백업합니다.")

    # 모든 문서 가져오기 (scroll API 사용)
    documents = []
    batch_size = 1000

    # 초기 검색
    response = await es.search(
        index=index_name,
        scroll='5m',
        size=batch_size,
        body={
            "query": {"match_all": {}},
            "_source": True
        }
    )

    scroll_id = response['_scroll_id']
    hits = response['hits']['hits']

    while hits:
        for hit in hits:
            doc = {
                "_id": hit['_id'],
                "_source": hit['_source']
            }
            documents.append(doc)

        if len(documents) % 10000 == 0:
            logger.info(f"진행 상황: {len(documents):,}/{total_docs:,}")

        # 다음 배치 가져오기
        response = await es.scroll(scroll_id=scroll_id, scroll='5m')
        scroll_id = response['_scroll_id']
        hits = response['hits']['hits']

    # scroll 정리
    await es.clear_scroll(scroll_id=scroll_id)

    # 메타데이터 추가
    backup_data = {
        "metadata": {
            "index_name": index_name,
            "backup_date": datetime.now().isoformat(),
            "total_documents": len(documents),
            "elasticsearch_version": (await es.info())['version']['number']
        },
        "documents": documents
    }

    # JSON 파일로 저장
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(backup_data, f, ensure_ascii=False, indent=2)

    logger.info(f"백업 완료! {len(documents):,}개 문서가 저장되었습니다.")
    logger.info(f"백업 파일: {output_file}")

    # 파일 크기 확인
    file_size_mb = output_file.stat().st_size / (1024 * 1024)
    logger.info(f"파일 크기: {file_size_mb:.2f} MB")


async def main():
    """메인 실행 함수"""
    es_host = os.getenv("ELASTICSEARCH_HOST", "localhost")
    es_port = int(os.getenv("ELASTICSEARCH_PORT", "9200"))

    es = AsyncElasticsearch(
        [f"http://{es_host}:{es_port}"],
        retry_on_timeout=True,
        max_retries=3
    )

    try:
        # Elasticsearch 연결 확인
        if not await es.ping():
            raise Exception("Elasticsearch 연결 실패!")

        logger.info("Elasticsearch 연결 성공!")

        # 백업 파일명 생성 (타임스탬프 포함)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_file = BACKUP_DIR / f"{INDEX_NAME}_backup_{timestamp}.json"

        # 백업 실행
        await backup_index(es, INDEX_NAME, backup_file)

        logger.info("\n✓ 백업이 완료되었습니다!")

    except Exception as e:
        logger.error(f"\n에러 발생: {e}", exc_info=True)
        raise
    finally:
        await es.close()


if __name__ == "__main__":
    asyncio.run(main())
