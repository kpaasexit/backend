import asyncio
import signal
import sys
from concurrent import futures
from typing import Optional

import grpc
from grpc_reflection.v1alpha import reflection

from app.config import get_settings
from app.core.logger import LoggerSetup
from app.grpc_service.quiz_servicer import QuizServicer

try:
    from protos.generated import quiz_service_pb2
    from protos.generated import quiz_service_pb2_grpc
except ImportError:
    quiz_service_pb2 = None
    quiz_service_pb2_grpc = None


class QuizGRPCServer:
    def __init__(self):
        self.settings = get_settings()
        self.logger = LoggerSetup.get_logger(__name__)
        self.server: Optional[grpc.Server] = None
        self.port = self.settings.server.quiz_service_port
        self._shutdown = False

    def _create_server(self) -> grpc.Server:
        thread_pool = futures.ThreadPoolExecutor(
            max_workers=10,
            thread_name_prefix="quiz_grpc"
        )

        server = grpc.server(
            thread_pool,
            options=[
                ('grpc.max_send_message_length', 50 * 1024 * 1024),
                ('grpc.max_receive_message_length', 50 * 1024 * 1024),
                ('grpc.max_metadata_size', 8 * 1024),
                ('grpc.keepalive_time_ms', 30000),
                ('grpc.keepalive_timeout_ms', 10000),
                ('grpc.keepalive_permit_without_calls', True),
                ('grpc.http2.max_pings_without_data', 0),
                ('grpc.http2.min_time_between_pings_ms', 10000),
            ]
        )

        quiz_servicer = QuizServicer()
        quiz_service_pb2_grpc.add_QuizServiceServicer_to_server(quiz_servicer, server)

        if not self.settings.is_production:
            SERVICE_NAMES = (
                quiz_service_pb2.DESCRIPTOR.services_by_name['QuizService'].full_name,
                reflection.SERVICE_NAME,
            )
            reflection.enable_server_reflection(SERVICE_NAMES, server)

        return server

    def start(self) -> None:
        self.server = self._create_server()

        address = f"[::]:{self.port}"
        self.server.add_insecure_port(address)

        self.server.start()
        self.logger.info(f"Quiz gRPC server started on port {self.port}")

    async def start_async(self) -> None:
        self.server = self._create_server()

        address = f"[::]:{self.port}"
        self.server.add_insecure_port(address)

        await self.server.start()
        self.logger.info(f"Quiz gRPC server started on port {self.port}")

        self._setup_signal_handlers()

        await self.server.wait_for_termination()

    def stop(self, grace_period: int = 30) -> None:
        if not self.server:
            return

        self.logger.info("Stopping Quiz gRPC server...")
        self._shutdown = True

        self.server.stop(grace_period)
        self.logger.info("Quiz gRPC server stopped")

    def _setup_signal_handlers(self) -> None:
        def signal_handler(sig, frame):
            self.logger.info(f"Received signal {sig}, shutting down...")
            self.stop()
            sys.exit(0)

        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)

    def health_check(self) -> bool:
        if not self.server or self._shutdown:
            return False

        return True


def create_quiz_grpc_server() -> QuizGRPCServer:
    if quiz_service_pb2_grpc is None:
        raise ImportError(
            "gRPC code not generated. Run: ./scripts/compile_protos.sh"
        )

    return QuizGRPCServer()


def run_quiz_grpc_server():
    server = create_quiz_grpc_server()
    server.start()
    asyncio.run(asyncio.Event().wait())


async def run_quiz_grpc_server_async():
    server = create_quiz_grpc_server()
    await server.start_async()