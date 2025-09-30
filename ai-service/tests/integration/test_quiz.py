#!/usr/bin/env python
"""Test quiz service via gRPC."""

import asyncio
import grpc
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent))

from protos.generated import nlp_service_pb2, nlp_service_pb2_grpc


async def test_quiz_grpc():
    channel = grpc.aio.insecure_channel('localhost:50051')
    stub = nlp_service_pb2_grpc.NLPServiceStub(channel)

    try:
        # 1. 퀴즈 목록 조회 먼저 실행하여 기존 퀴즈 확인
        print("1. 퀴즈 목록 조회 테스트")
        list_response = await stub.ListQuizzes(
            nlp_service_pb2.ListQuizzesRequest(category_id=1, limit=10)
        )
        print(f"   성공: {list_response.success}")
        print(f"   조회된 퀴즈 수: {len(list_response.quizzes)}")

        if list_response.quizzes:
            quiz_id = list_response.quizzes[0].quiz_id
            print(f"   첫 번째 퀴즈 ID: {quiz_id}")

            # 2. 퀴즈 조회
            print("\n2. 퀴즈 조회 테스트")
            get_response = await stub.GetQuiz(
                nlp_service_pb2.GetQuizRequest(quiz_id=quiz_id)
            )
            print(f"   성공: {get_response.success}")
            if get_response.success:
                print(f"   제목: {get_response.quiz.quiz_title}")
                print(f"   내용: {get_response.quiz.quiz_content}")

            # 3. 퀴즈 수정
            print("\n3. 퀴즈 수정 테스트")
            update_response = await stub.UpdateQuiz(
                nlp_service_pb2.UpdateQuizRequest(
                    quiz_id=quiz_id,
                    quiz_additional_information="테스트 수정 내용입니다."
                )
            )
            print(f"   성공: {update_response.success}")
            print(f"   메시지: {update_response.message}")

        # 4. 카테고리별 퀴즈 목록 조회
        print("\n4. 카테고리별 퀴즈 목록 조회 테스트")
        for category_id in [1, 2]:
            list_response = await stub.ListQuizzes(
                nlp_service_pb2.ListQuizzesRequest(category_id=category_id, limit=5)
            )
            print(f"   카테고리 {category_id}: {len(list_response.quizzes)}개 퀴즈")
            for quiz in list_response.quizzes[:2]:  # 처음 2개만 표시
                print(f"     - [{quiz.quiz_id}] {quiz.quiz_title}")

    except grpc.RpcError as e:
        print(f"gRPC 오류: {e.code()}: {e.details()}")
    except Exception as e:
        print(f"오류: {e}")
    finally:
        await channel.close()


if __name__ == "__main__":
    asyncio.run(test_quiz_grpc())