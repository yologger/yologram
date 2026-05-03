from pydantic import BaseModel


class EchoResponse(BaseModel):
    ip: str | None
    user_agent: str | None
    method: str
    uri: str
    headers: dict[str, str]
