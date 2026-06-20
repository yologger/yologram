from sqlalchemy.orm import Session

from app.domain.cms.enum import Section
from app.domain.cms.repository import PostCategoryRepository
from app.domain.cms.schema import PostCategoryResponse


class PostCategoryService:

    def __init__(self, db: Session):
        self.repository = PostCategoryRepository(db)

    def get_post_categories(self, section_path: str) -> list[PostCategoryResponse]:
        section = Section.from_path(section_path)
        categories = self.repository.find_active_by_section(section)
        return [PostCategoryResponse.model_validate(category) for category in categories]
