"""
Hugging Face Private Repository에 Elasticsearch 백업 파일 업로드
"""

import os
import sys
from pathlib import Path
from huggingface_hub import HfApi, create_repo
from dotenv import load_dotenv
import logging

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 프로젝트 루트에서 .env 로드
project_root = Path(__file__).parent.parent
env_path = project_root / ".env"
load_dotenv(env_path)

BACKUP_DIR = Path(__file__).parent / "backups"
HF_TOKEN = os.getenv("HUGGINGFACE_API_TOKEN")
HF_REPO_NAME = "elasticsearch-backups"  # Private repo 이름
HF_FIXED_FILENAME = "autocomplete_backup.json"  # 고정된 파일명 (덮어쓰기)


def upload_backup_to_huggingface(backup_file: Path):
    """
    백업 파일을 Hugging Face Private Repository에 업로드

    항상 고정된 파일명(autocomplete_backup.json)으로 덮어쓰기

    Args:
        backup_file: 업로드할 백업 파일 경로
    """
    if not HF_TOKEN:
        raise ValueError("HUGGINGFACE_API_TOKEN이 .env 파일에 설정되지 않았습니다.")

    if not backup_file.exists():
        raise FileNotFoundError(f"백업 파일을 찾을 수 없습니다: {backup_file}")

    logger.info("=" * 80)
    logger.info("Hugging Face 업로드 시작")
    logger.info("=" * 80)

    # Hugging Face API 초기화
    api = HfApi()

    # 사용자 정보 가져오기
    user_info = api.whoami(token=HF_TOKEN)
    username = user_info['name']
    repo_id = f"{username}/{HF_REPO_NAME}"

    logger.info(f"사용자: {username}")
    logger.info(f"Repository: {repo_id}")

    # Repository 생성 (이미 존재하면 무시)
    try:
        create_repo(
            repo_id=repo_id,
            repo_type="dataset",
            private=True,
            token=HF_TOKEN,
            exist_ok=True
        )
        logger.info(f"Repository 확인/생성 완료: {repo_id}")
    except Exception as e:
        logger.error(f"Repository 생성 실패: {e}")
        raise

    # 파일 업로드 (고정된 파일명으로 덮어쓰기)
    try:
        file_size_mb = backup_file.stat().st_size / (1024 * 1024)
        logger.info(f"로컬 파일: {backup_file.name} ({file_size_mb:.2f} MB)")
        logger.info(f"원격 파일명: {HF_FIXED_FILENAME} (덮어쓰기)")

        api.upload_file(
            path_or_fileobj=str(backup_file),
            path_in_repo=HF_FIXED_FILENAME,  # 고정된 파일명 사용
            repo_id=repo_id,
            repo_type="dataset",
            token=HF_TOKEN,
        )

        logger.info("✓ 업로드 완료!")
        logger.info(f"URL: https://huggingface.co/datasets/{repo_id}/blob/main/{HF_FIXED_FILENAME}")
        logger.info("=" * 80)

        return repo_id

    except Exception as e:
        logger.error(f"파일 업로드 실패: {e}")
        raise


def get_latest_backup() -> Path:
    """가장 최근 백업 파일 찾기"""
    if not BACKUP_DIR.exists():
        raise FileNotFoundError(f"백업 디렉토리를 찾을 수 없습니다: {BACKUP_DIR}")

    backup_files = sorted(BACKUP_DIR.glob("naver_kin_autocomplete_backup_*.json"), reverse=True)

    if not backup_files:
        raise FileNotFoundError(f"백업 파일을 찾을 수 없습니다: {BACKUP_DIR}")

    return backup_files[0]


def main():
    """메인 실행 함수"""
    import argparse

    parser = argparse.ArgumentParser(description="Hugging Face에 백업 파일 업로드")
    parser.add_argument("--file", type=str, help="업로드할 백업 파일 경로 (생략 시 최신 백업)")

    args = parser.parse_args()

    try:
        if args.file:
            backup_file = Path(args.file)
        else:
            backup_file = get_latest_backup()
            logger.info(f"최신 백업 파일 사용: {backup_file.name}")

        upload_backup_to_huggingface(backup_file)

    except Exception as e:
        logger.error(f"업로드 실패: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
