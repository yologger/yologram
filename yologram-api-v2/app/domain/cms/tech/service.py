from sqlalchemy.orm import Session

from app.domain.cms.tech.repository import TechCategoryRepository
from app.domain.cms.tech.schema import TechCategoryResponse


class TechCategoryService:

    def __init__(self, db: Session):
        self.repository = TechCategoryRepository(db)

    def get_categories(self) -> list[TechCategoryResponse]:
        categories = self.repository.find_active()
        return [TechCategoryResponse.model_validate(category) for category in categories]
