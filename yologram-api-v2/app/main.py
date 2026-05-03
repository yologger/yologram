from fastapi import FastAPI

from app.domain.test.router import router as test_router

app = FastAPI(title="yologram-api-v2")

app.include_router(test_router)
