from typing import Any

from pydantic import BaseModel


class ApiEnvelop(BaseModel):
    data: Any
