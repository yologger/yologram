from sqlalchemy.orm import Session

from app.domain.tech.category.repository import TechPostCategoryRepository
from app.domain.tech.category.schema import PostCategoryResponse


class TechPostCategoryService:

    def __init__(self, db: Session):
        self.repository = TechPostCategoryRepository(db)

    def get_post_categories(self) -> list[PostCategoryResponse]:
        categories = self.repository.find_active()
        return [PostCategoryResponse.model_validate(category) for category in categories]
