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
        # 1. 수동 퀴즈 생성
        print("1. 수동 퀴즈 생성 테스트")
        create_request = nlp_service_pb2.CreateQuizRequest(
            quiz_category_id=1,
            quiz_title="태양계의 행성 개수",
            quiz_content="태양계에는 총 8개의 행성이 있다.",
            quiz_type=nlp_service_pb2.OX,
            quiz_correct_answer="O",
            quiz_additional_information="2006년 명왕성이 왜행성으로 재분류되면서 태양계 행성은 8개가 되었습니다."
        )

        response = await stub.CreateQuiz(create_request)
        print(f"   성공: {response.success}")
        print(f"   메시지: {response.message}")

        if response.success:
            quiz_id = response.quiz.quiz_id
            print(f"   퀴즈 ID: {quiz_id}")
            print(f"   제목: {response.quiz.quiz_title}")

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
                    quiz_additional_information="명왕성은 이제 왜소행성으로 분류됩니다."
                )
            )
            print(f"   성공: {update_response.success}")
            print(f"   메시지: {update_response.message}")

        # 4. 퀴즈 목록 조회
        print("\n4. 퀴즈 목록 조회 테스트")
        list_response = await stub.ListQuizzes(
            nlp_service_pb2.ListQuizzesRequest(category_id=1, limit=10)
        )
        print(f"   성공: {list_response.success}")
        print(f"   조회된 퀴즈 수: {len(list_response.quizzes)}")
        for quiz in list_response.quizzes:
            print(f"   - [{quiz.quiz_id}] {quiz.quiz_title}")

        # 5. GPT를 사용한 퀴즈 생성 (선택사항)
        print("\n5. GPT 퀴즈 생성 테스트 (API 키가 설정된 경우)")
        generate_response = await stub.GenerateQuiz(
            nlp_service_pb2.GenerateQuizRequest(
                category_id=2,  # 역사
                count=1,
                quiz_type=nlp_service_pb2.FOUR_LIMBS
            )
        )
        print(f"   성공: {generate_response.success}")
        print(f"   메시지: {generate_response.message}")
        if generate_response.success and generate_response.quizzes:
            for quiz in generate_response.quizzes:
                print(f"   생성된 퀴즈: {quiz.quiz_title}")

    except grpc.RpcError as e:
        print(f"gRPC 오류: {e.code()}: {e.details()}")
    except Exception as e:
        print(f"오류: {e}")
    finally:
        await channel.close()


if __name__ == "__main__":
    asyncio.run(test_quiz_grpc())