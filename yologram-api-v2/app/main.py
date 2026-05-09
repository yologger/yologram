import logging

from fastapi import FastAPI

from app.config.logging import setup_logging
from app.config.metrics import setup_metrics
from app.config.settings import get_settings
from app.config.tracing import setup_tracing
from app.domain.test.router import router as test_router

setup_logging()
setup_metrics()

logger = logging.getLogger(__name__)
settings = get_settings()
logger.info(f"app_name={settings.app_name}, app_profile={settings.app_profile}")

app = FastAPI(title="yologram-api-v2")
setup_tracing(app)

app.include_router(test_router)
