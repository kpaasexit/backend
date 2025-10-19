"""Quiz scheduler configuration."""

from pydantic import Field
from .base import BaseConfig


class QuizSchedulerConfig(BaseConfig):
    """Quiz scheduler configuration."""

    quiz_scheduler_hour: int = Field(default=2, description="Hour to run quiz scheduler (0-23)")
    quiz_scheduler_minute: int = Field(default=0, description="Minute to run quiz scheduler (0-59)")

    @property
    def cron_hour(self) -> int:
        """Get hour for cron trigger."""
        return self.quiz_scheduler_hour

    @property
    def cron_minute(self) -> int:
        """Get minute for cron trigger."""
        return self.quiz_scheduler_minute
