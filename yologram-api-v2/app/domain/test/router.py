from fastapi import APIRouter, Depends, Request

from app.core.di import get_test_service
from app.domain.test.schema import EchoResponse
from app.domain.test.service import TestService

router = APIRouter(prefix="/api/v2/test")


@router.get("")
def index() -> str:
    return "/v2/test"


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
def profile(service: TestService = Depends(get_test_service)) -> str:
    return service.get_profile()


@router.get("/property")
def get_property(
    key: str,
    service: TestService = Depends(get_test_service),
) -> str | None:
    return service.get_property(key)
