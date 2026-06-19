import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config.logging import setup_logging
from app.config.metrics import setup_metrics
from app.config.settings import get_settings
from app.config.tracing import setup_tracing
from app.core.exception import register_exception_handlers
from app.domain.cms.router import router as cms_router
from app.domain.pms.router import router as pms_router
from app.domain.test.router import router as test_router
from app.domain.ums.auth_router import router as auth_router
from app.domain.ums.router import router as ums_router

setup_logging()
setup_metrics()

logger = logging.getLogger(__name__)
settings = get_settings()
logger.info(f"app_name={settings.app_name}, app_profile={settings.app_profile}")

app = FastAPI(title="yologram-api-v2", docs_url="/api/v2/docs", openapi_url="/api/v2/openapi.json")
setup_tracing(app)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

register_exception_handlers(app)

app.include_router(test_router)
app.include_router(ums_router)
app.include_router(auth_router)
app.include_router(cms_router)
app.include_router(pms_router)
