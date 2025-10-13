from enum import Enum
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class QuizType(str, Enum):
    OX = "OX"
    MULTIPLE = "MULTIPLE"


class QuizBase(BaseModel):
    quiz_category_id: int = Field(..., ge=1, le=8, description="Category ID (1-8)")
    quiz_title: str = Field(..., max_length=100, description="Quiz title")
    quiz_content: str = Field(..., description="Quiz content/question")
    quiz_type: QuizType = Field(..., description="Quiz type")
    quiz_correct_answer: int = Field(..., ge=0, le=3, description="Correct answer (OX: 0(정답), 1(오답) / MULTIPLE: 0-3)")
    explanation: Optional[str] = Field(None, description="Explanation for the correct answer")


class QuizCreate(QuizBase):
    pass


class QuizUpdate(BaseModel):
    quiz_category_id: Optional[int] = Field(None, ge=1, le=8)
    quiz_title: Optional[str] = Field(None, max_length=100)
    quiz_content: Optional[str] = None
    quiz_type: Optional[QuizType] = None
    quiz_correct_answer: Optional[int] = Field(None, ge=0, le=3)
    explanation: Optional[str] = None


class Quiz(QuizBase):
    quiz_id: int
    quiz_created_at: datetime
    quiz_updated_at: datetime

    class Config:
        from_attributes = True


class QuizGenerationRequest(BaseModel):
    category_id: int = Field(..., ge=1, le=8, description="Category ID for quiz generation")
    count: int = Field(default=1, ge=1, le=10, description="Number of quizzes to generate")
    quiz_type: Optional[QuizType] = Field(None, description="Type of quiz to generate")


class QuizGenerationPrompt(BaseModel):
    category: str
    quiz_type: QuizType
    existing_titles: list[str] = Field(default_factory=list)