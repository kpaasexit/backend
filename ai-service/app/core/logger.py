import logging
import sys
from pathlib import Path
from typing import Optional
import json
from datetime import datetime

from app.config import get_settings


class JSONFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        log_data = {
            "timestamp": datetime.utcnow().isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "module": record.module,
            "function": record.funcName,
            "line": record.lineno,
        }

        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        if hasattr(record, "extra_fields"):
            log_data.update(record.extra_fields)

        return json.dumps(log_data, ensure_ascii=False)


class ColoredFormatter(logging.Formatter):
    COLORS = {
        'DEBUG': '\033[36m',    # Cyan
        'INFO': '\033[32m',     # Green
        'WARNING': '\033[33m',  # Yellow
        'ERROR': '\033[31m',    # Red
        'CRITICAL': '\033[35m', # Magenta
    }
    RESET = '\033[0m'

    def format(self, record: logging.LogRecord) -> str:
        levelname = record.levelname
        if levelname in self.COLORS:
            record.levelname = f"{self.COLORS[levelname]}{levelname}{self.RESET}"
        return super().format(record)


class LoggerSetup:
    @staticmethod
    def setup_logger(
        name: str = "nlp_service",
        log_level: Optional[str] = None,
        log_file: Optional[Path] = None,
        use_json: bool = False
    ) -> logging.Logger:
        settings = get_settings()
        log_level = log_level or settings.server.log_level

        logger = logging.getLogger(name)
        logger.setLevel(getattr(logging, log_level.upper()))
        logger.handlers = []
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(getattr(logging, log_level.upper()))

        if settings.is_production or use_json:
            console_formatter = JSONFormatter()
        else:
            console_formatter = ColoredFormatter(
                "%(asctime)s | %(levelname)-8s | %(message)s",
                datefmt="%H:%M:%S"
            )
        console_handler.setFormatter(console_formatter)
        logger.addHandler(console_handler)

        if log_file:
            log_file.parent.mkdir(parents=True, exist_ok=True)
            file_handler = logging.FileHandler(log_file)
            file_handler.setLevel(getattr(logging, log_level.upper()))

            file_formatter = JSONFormatter()
            file_handler.setFormatter(file_formatter)
            logger.addHandler(file_handler)

        return logger

    @staticmethod
    def get_logger(name: str) -> logging.Logger:
        logger = logging.getLogger(name)
        if not logger.handlers:
            LoggerSetup.setup_logger(name)
        return logger


class LogContext:
    def __init__(self, logger: logging.Logger, **kwargs):
        self.logger = logger
        self.extra_fields = kwargs
        self.old_factory = None

    def __enter__(self):
        self.old_factory = logging.getLogRecordFactory()

        def record_factory(*args, **kwargs):
            record = self.old_factory(*args, **kwargs)
            record.extra_fields = self.extra_fields
            return record

        logging.setLogRecordFactory(record_factory)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        logging.setLogRecordFactory(self.old_factory)


def log_execution_time(func):
    import time
    from functools import wraps

    @wraps(func)
    def wrapper(*args, **kwargs):
        logger = LoggerSetup.get_logger(func.__module__)
        start_time = time.time()

        try:
            result = func(*args, **kwargs)
            execution_time = time.time() - start_time
            logger.debug(
                f"Function '{func.__name__}' executed successfully",
                extra={"execution_time": execution_time}
            )
            return result
        except Exception as e:
            execution_time = time.time() - start_time
            logger.error(
                f"Function '{func.__name__}' failed",
                extra={"execution_time": execution_time, "error": str(e)}
            )
            raise

    return wrapper


def log_memory_usage(func):
    import psutil
    from functools import wraps

    @wraps(func)
    def wrapper(*args, **kwargs):
        logger = LoggerSetup.get_logger(func.__module__)
        process = psutil.Process()

        mem_before = process.memory_info().rss / 1024 / 1024

        result = func(*args, **kwargs)

        mem_after = process.memory_info().rss / 1024 / 1024
        mem_delta = mem_after - mem_before

        logger.debug(
            f"Function '{func.__name__}' memory usage",
            extra={
                "memory_before_mb": mem_before,
                "memory_after_mb": mem_after,
                "memory_delta_mb": mem_delta
            }
        )
        return result

    return wrapper


logger = LoggerSetup.get_logger("nlp_service")