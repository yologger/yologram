from pydantic import BaseModel, ConfigDict, Field


class PostCategoryResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    id: int
    name: str
    sort_order: int = Field(serialization_alias="sortOrder")
