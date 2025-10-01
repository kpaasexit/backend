"""Simple Eureka service registration client."""

import socket
import logging
import httpx
from app.config import get_settings

logger = logging.getLogger(__name__)


class EurekaClient:
    """Simple Eureka registration client."""

    def __init__(self):
        self.settings = get_settings()
        self.eureka_config = self.settings.eureka
        self._initialized = False

    def _get_instance_id(self) -> str:
        """Generate instance ID."""
        if self.eureka_config.eureka_instance_id:
            return self.eureka_config.eureka_instance_id
        hostname = socket.gethostname()
        return f"{hostname}:{self.eureka_config.eureka_app_name}:{self.settings.server.http_port}"

    def register(self) -> bool:
        """Register with Eureka server."""
        if not self.eureka_config.eureka_enable:
            logger.info("Eureka is disabled, skipping registration")
            return False

        try:
            instance_id = self._get_instance_id()
            registration_data = {
                "instance": {
                    "instanceId": instance_id,
                    "hostName": self.eureka_config.eureka_hostname,
                    "app": self.eureka_config.eureka_app_name,
                    "ipAddr": self.eureka_config.eureka_hostname,
                    "status": "UP",
                    "port": {
                        "$": self.settings.server.http_port,
                        "@enabled": "true"
                    },
                    "healthCheckUrl": f"http://{self.eureka_config.eureka_hostname}:{self.settings.server.http_port}/health",
                    "statusPageUrl": f"http://{self.eureka_config.eureka_hostname}:{self.settings.server.http_port}/",
                    "homePageUrl": f"http://{self.eureka_config.eureka_hostname}:{self.settings.server.http_port}/",
                    "dataCenterInfo": {
                        "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                        "name": "MyOwn"
                    },
                    "metadata": {
                        "management.port": str(self.settings.server.http_port),
                        "grpc.port": str(self.settings.server.grpc_port)
                    }
                }
            }

            url = f"{self.eureka_config.eureka_server_url}/apps/{self.eureka_config.eureka_app_name}"
            headers = {"Content-Type": "application/json"}

            with httpx.Client() as client:
                response = client.post(url, json=registration_data, headers=headers)

            if response.status_code == 204:
                logger.info(f"Successfully registered with Eureka: {self.eureka_config.eureka_app_name}")
                self._initialized = True
                return True
            else:
                logger.error(f"Failed to register with Eureka: {response.status_code}")
                return False

        except Exception as e:
            logger.error(f"Error registering with Eureka: {e}")
            return False

    def deregister(self) -> bool:
        """Deregister from Eureka server."""
        if not self._initialized:
            return False

        try:
            instance_id = self._get_instance_id()
            url = f"{self.eureka_config.eureka_server_url}/apps/{self.eureka_config.eureka_app_name}/{instance_id}"

            with httpx.Client() as client:
                response = client.delete(url)

            if response.status_code == 200:
                logger.info("Successfully deregistered from Eureka")
                self._initialized = False
                return True
            else:
                logger.error(f"Failed to deregister from Eureka: {response.status_code}")
                return False

        except Exception as e:
            logger.error(f"Error deregistering from Eureka: {e}")
            return False


# Singleton instance
_eureka_client = None


def get_eureka_client() -> EurekaClient:
    """Get Eureka client singleton instance."""
    global _eureka_client
    if _eureka_client is None:
        _eureka_client = EurekaClient()
    return _eureka_client