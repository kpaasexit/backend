from sqlalchemy import Column, BigInteger, SmallInteger, String, Text, Enum, DateTime, func
from sqlalchemy.sql import text
from app.db.database import Base
import enum


class QuizTypeEnum(enum.Enum):
    FOUR_LIMBS = "FOUR_LIMBS"
    OX = "OX"


class QuizModel(Base):
    __tablename__ = "quizs"
    __table_args__ = {'extend_existing': True}  # Allow model to work with existing table

    quiz_id = Column(BigInteger, primary_key=True, autoincrement=True)
    quiz_created_at = Column(DateTime(6), nullable=True, default=func.now())
    quiz_updated_at = Column(DateTime(6), nullable=True, default=func.now(), onupdate=func.now())
    quiz_additional_information = Column(Text, nullable=True)
    quiz_content = Column(Text, nullable=True)
    quiz_correct_answer = Column(String(1), nullable=True)
    quiz_title = Column(String(100), nullable=True)
    quiz_type = Column(Enum(QuizTypeEnum), nullable=True)
    quiz_category_id = Column(SmallInteger, nullable=True)