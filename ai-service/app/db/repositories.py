from typing import Optional, List
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from sqlalchemy.exc import SQLAlchemyError
from app.db.models import QuizModel, QuizTypeEnum
from app.models.quiz import Quiz, QuizCreate, QuizUpdate, QuizType
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


class QuizRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create(self, quiz_data: QuizCreate) -> Quiz:
        try:
            quiz_type_value = quiz_data.quiz_type.value
            if quiz_type_value == "MULTIPLE":
                quiz_type_enum = QuizTypeEnum.FOUR_LIMBS
            else:
                quiz_type_enum = QuizTypeEnum[quiz_type_value]

            db_quiz = QuizModel(
                quiz_category_id=quiz_data.quiz_category_id,
                quiz_title=quiz_data.quiz_title,
                quiz_content=quiz_data.quiz_content,
                quiz_type=quiz_type_enum,
                quiz_correct_answer=quiz_data.quiz_correct_answer,
                quiz_additional_information=quiz_data.explanation,  # Map explanation to MySQL column
                quiz_created_at=datetime.now(),
                quiz_updated_at=datetime.now()
            )

            self.db.add(db_quiz)
            await self.db.commit()
            await self.db.refresh(db_quiz)

            logger.info(f"Quiz created successfully with ID: {db_quiz.quiz_id}")
            return self._to_domain_model(db_quiz)
        except SQLAlchemyError as e:
            await self.db.rollback()
            if "Duplicate entry" in str(e):
                logger.error(f"Duplicate quiz entry attempted: {e}")
                raise ValueError("Duplicate quiz entry")
            elif "Data too long" in str(e):
                logger.error(f"Data exceeds column size limit: {e}")
                raise ValueError("Quiz data exceeds size limits")
            else:
                logger.error(f"Database error creating quiz: {e}")
                raise

    async def get_by_id(self, quiz_id: int) -> Optional[Quiz]:
        try:
            result = await self.db.execute(
                select(QuizModel).where(QuizModel.quiz_id == quiz_id)
            )
            db_quiz = result.scalar_one_or_none()
            return self._to_domain_model(db_quiz) if db_quiz else None
        except SQLAlchemyError as e:
            logger.error(f"Failed to get quiz by id {quiz_id}: {e}")
            raise

    async def update(self, quiz_id: int, quiz_update: QuizUpdate) -> Optional[Quiz]:
        try:
            result = await self.db.execute(
                select(QuizModel).where(QuizModel.quiz_id == quiz_id)
            )
            db_quiz = result.scalar_one_or_none()

            if not db_quiz:
                return None

            update_data = quiz_update.model_dump(exclude_unset=True)

            # Map quiz_type for MySQL compatibility
            if 'quiz_type' in update_data and update_data['quiz_type']:
                quiz_type_value = update_data['quiz_type'].value
                if quiz_type_value == "MULTIPLE":
                    update_data['quiz_type'] = QuizTypeEnum.FOUR_LIMBS
                else:
                    update_data['quiz_type'] = QuizTypeEnum[quiz_type_value]

            # Map explanation to quiz_additional_information
            if 'explanation' in update_data:
                update_data['quiz_additional_information'] = update_data.pop('explanation')

            for field, value in update_data.items():
                setattr(db_quiz, field, value)

            db_quiz.quiz_updated_at = datetime.now()

            await self.db.commit()
            await self.db.refresh(db_quiz)

            return self._to_domain_model(db_quiz)
        except SQLAlchemyError as e:
            await self.db.rollback()
            logger.error(f"Failed to update quiz {quiz_id}: {e}")
            raise

    async def list_quizzes(
        self,
        category_id: Optional[int] = None,
        limit: int = 100
    ) -> List[Quiz]:
        try:
            query = select(QuizModel)

            if category_id is not None:
                query = query.where(QuizModel.quiz_category_id == category_id)

            query = query.order_by(QuizModel.quiz_created_at.desc()).limit(limit)

            result = await self.db.execute(query)
            db_quizzes = result.scalars().all()

            return [self._to_domain_model(quiz) for quiz in db_quizzes]
        except SQLAlchemyError as e:
            logger.error(f"Failed to list quizzes: {e}")
            raise

    async def create_batch(self, quizzes: List[QuizCreate]) -> List[Quiz]:
        try:
            db_quizzes = []
            for quiz_data in quizzes:
                # Map MULTIPLE to FOUR_LIMBS for MySQL compatibility
                quiz_type_value = quiz_data.quiz_type.value
                if quiz_type_value == "MULTIPLE":
                    quiz_type_enum = QuizTypeEnum.FOUR_LIMBS
                else:
                    quiz_type_enum = QuizTypeEnum[quiz_type_value]

                db_quiz = QuizModel(
                    quiz_category_id=quiz_data.quiz_category_id,
                    quiz_title=quiz_data.quiz_title,
                    quiz_content=quiz_data.quiz_content,
                    quiz_type=quiz_type_enum,
                    quiz_correct_answer=quiz_data.quiz_correct_answer,
                    quiz_additional_information=quiz_data.explanation,  # Map explanation to MySQL column
                    quiz_created_at=datetime.now(),
                    quiz_updated_at=datetime.now()
                )
                self.db.add(db_quiz)
                db_quizzes.append(db_quiz)

            await self.db.commit()

            for db_quiz in db_quizzes:
                await self.db.refresh(db_quiz)

            return [self._to_domain_model(quiz) for quiz in db_quizzes]
        except SQLAlchemyError as e:
            await self.db.rollback()
            logger.error(f"Failed to create batch of quizzes: {e}")
            raise

    def _to_domain_model(self, db_quiz: QuizModel) -> Quiz:
        # Map FOUR_LIMBS back to MULTIPLE for domain model
        quiz_type_value = db_quiz.quiz_type.value
        if quiz_type_value == "FOUR_LIMBS":
            quiz_type = QuizType.MULTIPLE
        else:
            quiz_type = QuizType(quiz_type_value)

        return Quiz(
            quiz_id=db_quiz.quiz_id,
            quiz_category_id=db_quiz.quiz_category_id,
            quiz_title=db_quiz.quiz_title,
            quiz_content=db_quiz.quiz_content,
            quiz_type=quiz_type,
            quiz_correct_answer=db_quiz.quiz_correct_answer,
            explanation=db_quiz.quiz_additional_information,  # Map MySQL column to explanation
            quiz_created_at=db_quiz.quiz_created_at,
            quiz_updated_at=db_quiz.quiz_updated_at
        )