from sqlalchemy import Column, BigInteger, SmallInteger, String, Text, Enum, DateTime, func
from sqlalchemy.sql import text
from app.db.database import Base
import enum


class QuizTypeEnum(enum.Enum):
    MULTIPLE = "MULTIPLE"
    OX = "OX"


class QuizModel(Base):
    __tablename__ = "quizs"
    __table_args__ = {'extend_existing': True}  # Allow model to work with existing table

    quiz_id = Column(BigInteger, primary_key=True, autoincrement=True)
    quiz_created_at = Column(DateTime(6), nullable=True, default=func.now())
    quiz_updated_at = Column(DateTime(6), nullable=True, default=func.now(), onupdate=func.now())
    quiz_additional_information = Column(Text, nullable=True)  # MySQL column name
    quiz_content = Column(Text, nullable=True)
    quiz_correct_answer = Column(SmallInteger, nullable=True)  # OX: 0(정답), 1(오답) / MULTIPLE: 0-3 (선택지 인덱스)
    quiz_title = Column(String(100), nullable=True)
    quiz_type = Column(Enum(QuizTypeEnum), nullable=True)
    quiz_category_id = Column(SmallInteger, nullable=True)