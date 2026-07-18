import logging

from fastapi import APIRouter, Request

from app.config.settings import get_settings

logger = logging.getLogger(__name__)
from app.domain.test.schema import EchoResponse
from app.domain.test.service import TestService

router = APIRouter(prefix="/api/v2/test", tags=["Test"])
service = TestService(settings=get_settings())


@router.get("")
def index() -> str:
    logger.info("GET /api/v2/test")
    return "/api/v2/test"


@router.get("/echo")
def echo(request: Request) -> EchoResponse:
    headers = {k: v for k, v in request.headers.items()}
    return EchoResponse(
        ip=request.client.host if request.client else None,
        user_agent=request.headers.get("user-agent"),
        method=request.method,
        uri=str(request.url.path),
        headers=headers,
    )


@router.get("/profile")
def profile() -> str:
    return service.get_profile()


@router.get("/property")
def get_property(key: str) -> str | None:
    return service.get_property(key)
