from sqlalchemy.orm import Session

from app.core.exception import TargetPostNotFoundException
from app.domain.comment.model import Comment
from app.domain.comment.post_query_client import LocalPostQueryClient, PostQueryClient
from app.domain.comment.repository import CommentRepository
from app.domain.comment.schema import CreateCommentRequest, CreateCommentResponse


class CommentService:

    def __init__(self, db: Session):
        self.comment_repository = CommentRepository(db)
        self.post_query_client: PostQueryClient = LocalPostQueryClient(db)

    def create(self, post_id: int, user_id: int, request: CreateCommentRequest) -> CreateCommentResponse:
        # 대상 글이 없으면 404 (고아 댓글 방지)
        if not self.post_query_client.exists(post_id):
            raise TargetPostNotFoundException()

        comment = self.comment_repository.save(
            Comment(
                post_id=post_id,
                user_id=user_id,
                content=request.content,
            )
        )
        return CreateCommentResponse(id=comment.id)
