"""
Elasticsearch 인덱스 복원 스크립트

백업된 JSON 파일로부터 인덱스를 복원합니다.
"""

import asyncio
import os
import json
from pathlib import Path
from typing import List, Dict, Any

from elasticsearch import AsyncElasticsearch

import logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


async def restore_index(
    es: AsyncElasticsearch,
    backup_file: Path,
    target_index: str = None,
    overwrite: bool = False
):
    """백업 파일로부터 인덱스 복원"""

    logger.info(f"백업 파일 로드 중: {backup_file}")

    # 백업 파일 읽기
    with open(backup_file, 'r', encoding='utf-8') as f:
        backup_data = json.load(f)

    metadata = backup_data['metadata']
    documents = backup_data['documents']

    logger.info(f"백업 정보:")
    logger.info(f"  - 원본 인덱스: {metadata['index_name']}")
    logger.info(f"  - 백업 날짜: {metadata['backup_date']}")
    logger.info(f"  - 문서 수: {metadata['total_documents']:,}")

    # 타겟 인덱스 이름 결정
    if target_index is None:
        target_index = metadata['index_name']

    logger.info(f"\n복원 대상 인덱스: {target_index}")

    # 인덱스 존재 확인
    index_exists = await es.indices.exists(index=target_index)

    if index_exists:
        if overwrite:
            logger.warning(f"인덱스 '{target_index}'가 이미 존재합니다. 삭제 후 복원합니다.")
            await es.indices.delete(index=target_index)
        else:
            logger.error(f"인덱스 '{target_index}'가 이미 존재합니다.")
            logger.error("덮어쓰려면 --overwrite 옵션을 사용하세요.")
            return

    # 문서 복원
    logger.info(f"\n{len(documents):,}개 문서 복원 시작...")

    success_count = 0
    fail_count = 0
    batch_size = 1000

    for i in range(0, len(documents), batch_size):
        batch = documents[i:i + batch_size]

        bulk_body = []
        for doc in batch:
            action = {"index": {"_index": target_index, "_id": doc["_id"]}}
            bulk_body.append(action)
            bulk_body.append(doc["_source"])

        try:
            response = await es.bulk(operations=bulk_body, refresh=False)

            if response.get("errors"):
                for item in response.get("items", []):
                    if "index" in item:
                        if item["index"].get("status") in [200, 201]:
                            success_count += 1
                        else:
                            fail_count += 1
            else:
                success_count += len(batch)

            if (i + batch_size) % 10000 < batch_size:
                logger.info(f"진행 상황: {min(i + batch_size, len(documents)):,}/{len(documents):,}")

        except Exception as e:
            fail_count += len(batch)
            logger.error(f"배치 복원 에러 (위치: {i}): {e}")

    # 인덱스 리프레시
    await es.indices.refresh(index=target_index)

    logger.info(f"\n복원 완료!")
    logger.info(f"  - 성공: {success_count:,}개")
    logger.info(f"  - 실패: {fail_count}개")

    # 복원된 문서 수 확인
    count_response = await es.count(index=target_index)
    restored_count = count_response['count']
    logger.info(f"  - 복원된 총 문서 수: {restored_count:,}개")


async def list_backups(backup_dir: Path):
    """사용 가능한 백업 파일 목록 출력"""
    if not backup_dir.exists():
        logger.warning(f"백업 디렉토리가 존재하지 않습니다: {backup_dir}")
        return

    backup_files = sorted(backup_dir.glob("*.json"), reverse=True)

    if not backup_files:
        logger.info("백업 파일이 없습니다.")
        return

    logger.info(f"\n사용 가능한 백업 파일 ({len(backup_files)}개):")
    for i, backup_file in enumerate(backup_files, 1):
        file_size_mb = backup_file.stat().st_size / (1024 * 1024)
        logger.info(f"  {i}. {backup_file.name} ({file_size_mb:.2f} MB)")


async def main():
    """메인 실행 함수"""
    import argparse

    parser = argparse.ArgumentParser(description="Elasticsearch 인덱스 복원")
    parser.add_argument("backup_file", nargs='?', help="복원할 백업 파일 경로")
    parser.add_argument("--list", action="store_true", help="사용 가능한 백업 파일 목록 출력")
    parser.add_argument("--target", help="복원할 타겟 인덱스 이름 (기본: 백업 파일의 원본 인덱스)")
    parser.add_argument("--overwrite", action="store_true", help="기존 인덱스 덮어쓰기")

    args = parser.parse_args()

    backup_dir = Path(__file__).parent / "backups"

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

        # 백업 파일 목록 출력
        if args.list:
            await list_backups(backup_dir)
            return

        # 백업 파일 확인
        if not args.backup_file:
            logger.error("백업 파일을 지정해주세요.")
            logger.info("\n사용법:")
            logger.info("  python restore_index.py <백업파일경로> [--target 인덱스명] [--overwrite]")
            logger.info("  python restore_index.py --list  # 백업 파일 목록 확인")
            await list_backups(backup_dir)
            return

        backup_file = Path(args.backup_file)

        if not backup_file.exists():
            logger.error(f"백업 파일을 찾을 수 없습니다: {backup_file}")
            return

        # 복원 실행
        await restore_index(
            es,
            backup_file,
            target_index=args.target,
            overwrite=args.overwrite
        )

        logger.info("\n✓ 복원이 완료되었습니다!")

    except Exception as e:
        logger.error(f"\n에러 발생: {e}", exc_info=True)
        raise
    finally:
        await es.close()


if __name__ == "__main__":
    asyncio.run(main())
