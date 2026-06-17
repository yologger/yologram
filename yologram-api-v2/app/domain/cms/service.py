from sqlalchemy.orm import Session

from app.domain.cms.enum import Section
from app.domain.cms.repository import CategoryRepository
from app.domain.cms.schema import CategoryResponse


class CategoryService:

    def __init__(self, db: Session):
        self.repository = CategoryRepository(db)

    def get_categories(self, section_path: str) -> list[CategoryResponse]:
        section = Section.from_path(section_path)
        categories = self.repository.find_active_by_section(section)
        return [CategoryResponse.model_validate(category) for category in categories]
