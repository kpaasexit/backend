"""Base handler class for gRPC operations."""

import time
import traceback
from typing import Any

import grpc

from app.core.logger import LoggerSetup
from app.core.exceptions import NLPServiceError



logger = LoggerSetup.get_logger(__name__)


class BaseHandler:
    """Base class for gRPC request handlers."""

    def __init__(self):
        self.logger = logger

    def handle_error(self, context, operation: str, e: Exception) -> None:
        """Handle errors and set appropriate gRPC context."""
        error_msg = f"{operation} failed: {str(e)}"

        if isinstance(e, NLPServiceError):
            self.logger.error(error_msg)
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(str(e))
        else:
            self.logger.error(f"{error_msg}\n{traceback.format_exc()}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details("Internal server error")

    def log_request(self, operation: str, **kwargs):
        """Log request details."""
        details = ", ".join(f"{k}={v}" for k, v in kwargs.items())
        self.logger.info(f"[gRPC] {operation}: {details}")

    def log_performance(self, operation: str, start_time: float, **metrics):
        """Log performance metrics."""
        duration = time.time() - start_time
        metrics_str = ", ".join(f"{k}={v}" for k, v in metrics.items())
        self.logger.info(f"[gRPC] {operation} completed in {duration:.3f}s: {metrics_str}")