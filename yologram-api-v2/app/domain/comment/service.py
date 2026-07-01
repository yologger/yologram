from sqlalchemy.orm import Session

from app.core.exception import TargetPostNotFoundException
from app.core.response import ApiEnvelopCursorPage, ApiEnvelopPage
from app.domain.comment.cursor import CommentCursor
from app.domain.comment.model import Comment
from app.domain.comment.post_query_client import LocalPostQueryClient, PostQueryClient
from app.domain.comment.repository import CommentRepository
from app.domain.comment.schema import (
    CommentAuthor,
    CommentResponse,
    CreateCommentRequest,
    CreateCommentResponse,
)
from app.domain.comment.sort import CommentSort
from app.domain.comment.user_query_client import LocalUserQueryClient, UserQueryClient

MAX_PAGE_SIZE = 50


class CommentService:

    def __init__(self, db: Session):
        self.comment_repository = CommentRepository(db)
        self.post_query_client: PostQueryClient = LocalPostQueryClient(db)
        self.user_query_client: UserQueryClient = LocalUserQueryClient(db)

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

    def get_comments_by_cursor(
        self, post_id: int, sort_param: str | None, cursor: str | None, size: int
    ) -> ApiEnvelopCursorPage[CommentResponse]:
        """특정 글의 댓글 목록 (cursor 페이지네이션) — 실사용. 최신순(기본)/오래된순 keyset.
        없는 post_id면 빈 목록을 반환한다(존재 검증은 작성 시에만)."""
        sort = CommentSort.from_param(sort_param)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        cursor_id = CommentCursor.decode(cursor) if cursor else None

        comments = self.comment_repository.find_by_post_cursor(post_id, sort, cursor_id, page_size)

        data = self._to_responses(comments)
        next_cursor = CommentCursor.encode(comments[-1].id) if comments else None
        return ApiEnvelopCursorPage(data=data, next_cursor=next_cursor)

    def get_comments_by_offset(
        self, post_id: int, sort_param: str | None, page: int, size: int
    ) -> ApiEnvelopPage[CommentResponse]:
        """특정 글의 댓글 목록 (offset 페이지네이션) — 학습용.
        cursor 방식(get_comments_by_cursor)과 대비되는 offset + 전체 count 예시. 엔드포인트는 비활성(router 주석)."""
        sort = CommentSort.from_param(sort_param)
        page_number = max(0, page)
        page_size = max(1, min(size, MAX_PAGE_SIZE))
        offset = page_number * page_size

        total_count = self.comment_repository.count_by_post(post_id)
        comments = self.comment_repository.find_by_post_offset(post_id, sort, offset, page_size)

        data = self._to_responses(comments)
        total_pages = 0 if total_count == 0 else (total_count + page_size - 1) // page_size
        return ApiEnvelopPage(
            data=data,
            page=page_number,
            size=page_size,
            total_pages=total_pages,
            total_count=total_count,
            first=(page_number == 0),
            last=(total_pages == 0 or page_number >= total_pages - 1),
        )

    def _to_responses(self, comments: list[Comment]) -> list[CommentResponse]:
        """댓글 목록 → CommentResponse. 작성자 닉네임 배치 조회(N+1 회피)를 공유."""
        nicknames = self.user_query_client.find_nicknames([c.user_id for c in comments])
        return [
            CommentResponse(
                id=comment.id,
                post_id=comment.post_id,
                author=CommentAuthor(uid=comment.user_id, nickname=nicknames.get(comment.user_id)),
                content=comment.content,
                created_at=comment.created_at,
            )
            for comment in comments
        ]
