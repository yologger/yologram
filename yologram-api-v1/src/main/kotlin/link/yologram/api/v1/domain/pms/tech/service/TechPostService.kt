package link.yologram.api.v1.domain.pms.tech.service

import link.yologram.api.v1.domain.pms.tech.entity.TechPost
import link.yologram.api.v1.domain.pms.tech.entity.TechPostCategoryMapping
import link.yologram.api.v1.domain.pms.tech.exception.InvalidTechCategoryException
import link.yologram.api.v1.domain.pms.tech.exception.InvalidTechSectionException
import link.yologram.api.v1.domain.pms.tech.exception.TechPostForbiddenException
import link.yologram.api.v1.domain.pms.tech.exception.TechPostNotFoundException
import link.yologram.api.v1.domain.pms.tech.model.CreateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.model.CreateTechPostResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostCursor
import link.yologram.api.v1.domain.pms.tech.model.TechPostDetailResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostSummaryResponse
import link.yologram.api.v1.domain.pms.tech.model.UpdateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.repository.TechPostCategoryMappingRepository
import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import link.yologram.api.v1.infra.client.cms.CmsApiClient
import link.yologram.api.v1.infra.client.comment.CommentApiClient
import link.yologram.api.v1.infra.client.ums.UmsApiClient
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TechPostService(
    private val postRepository: TechPostRepository,
    private val postCategoryMappingRepository: TechPostCategoryMappingRepository,
    private val cmsApiClient: CmsApiClient,
    private val umsApiClient: UmsApiClient,
    private val commentApiClient: CommentApiClient,
) {

    companion object {
        private const val MAX_PAGE_SIZE = 50

        /**
         * 내 글 목록의 section 쿼리 파라미터 검증 (구 Section.fromPath와 동일 응답 유지).
         * 테이블 분리 후 테크 도메인만 존재 — null(전체) 또는 "tech"만 허용, 그 외 400.
         */
        private fun validateSectionParam(sectionPath: String?) {
            if (sectionPath != null && !sectionPath.equals("tech", ignoreCase = true)) {
                throw InvalidTechSectionException()
            }
        }
    }

    // 게시글 단건 생성
    @Transactional
    fun create(userId: Long, request: CreateTechPostRequest): CreateTechPostResponse {
        val categoryIds = request.categoryIds.toSet()

        if (!cmsApiClient.allActive(categoryIds)) {
            throw InvalidTechCategoryException()
        }

        val post = postRepository.save(
            TechPost(
                userId = userId,
                title = request.title?.takeIf { it.isNotBlank() },
                content = request.content!!,
            )
        )

        categoryIds.forEach { categoryId ->
            postCategoryMappingRepository.save(TechPostCategoryMapping(postId = post.id, categoryId = categoryId))
        }

        return CreateTechPostResponse(id = post.id)
    }

    // 게시글 수정 (본인 글)
    @Transactional
    fun update(id: Long, userId: Long, request: UpdateTechPostRequest) {
        // 없는 글이면 404
        val post = postRepository.findByIdOrNull(id) ?: throw TechPostNotFoundException()

        // 작성자 본인만 수정 가능 (아니면 403)
        if (post.userId != userId) throw TechPostForbiddenException()

        // 카테고리 검증 (작성과 동일: 테크 게시판 활성 카테고리 1~3개)
        val categoryIds = request.categoryIds.toSet()
        if (!cmsApiClient.allActive(categoryIds)) {
            throw InvalidTechCategoryException()
        }

        // 제목·내용 갱신 (JPA 더티체킹 → flush 시 update, modifiedDate 자동 갱신)
        post.update(request.title?.takeIf { it.isNotBlank() }, request.content!!)

        // 카테고리 매핑은 전체 교체 (기존 제거 후 재생성) — @Modifying 벌크 delete라 uk 충돌 없음
        postCategoryMappingRepository.deleteByPostId(post.id)
        categoryIds.forEach { categoryId ->
            postCategoryMappingRepository.save(TechPostCategoryMapping(postId = post.id, categoryId = categoryId))
        }
    }

    // 게시글 삭제 (본인 글)
    @Transactional
    fun delete(id: Long, userId: Long) {
        // 없는 글이면 404
        val post = postRepository.findByIdOrNull(id) ?: throw TechPostNotFoundException()

        // 작성자 본인만 삭제 가능 (아니면 403)
        if (post.userId != userId) throw TechPostForbiddenException()

        // 연관 데이터 정리 후 게시글 삭제 — 카테고리 매핑 + 댓글(고아 방지, CommentApiClient로 경계 추상화).
        // 같은 트랜잭션이라 글·매핑·댓글 삭제가 원자적 (좋아요 도메인은 미구현)
        postCategoryMappingRepository.deleteByPostId(post.id)
        commentApiClient.deleteByPostId(post.id)
        postRepository.delete(post)
    }

    // 게시글 단건 조회
    @Transactional(readOnly = true)
    fun getPost(id: Long): TechPostDetailResponse {
        // 댓글 수는 tech_post_comment_count leftJoin + coalesce(0) — count row가 없으면 0
        val postWithCount = postRepository.findPostWithCommentCount(id) ?: throw TechPostNotFoundException()
        val post = postWithCount.post

        val categoryIds = postCategoryMappingRepository.findByPostId(post.id).map { it.categoryId }
        val nickname = umsApiClient.findNickname(post.userId)

        return TechPostDetailResponse(
            id = post.id,
            author = TechPostDetailResponse.Author(uid = post.userId, nickname = nickname),
            title = post.title,
            content = post.content,
            categoryIds = categoryIds,
            likeCount = post.likeCount,
            commentCount = postWithCount.commentCount.toInt(),
            createdAt = post.createdAt,
        )
    }

    /**
     * 테크 피드 목록 조회 (cursor 페이지네이션)
     */
    @Transactional(readOnly = true)
    fun getPostsByCursor(categoryId: Long?, cursor: String?, size: Int): ApiEnvelopCursorPage<TechPostSummaryResponse> {

        // 1) size 보정: 0·음수·과도한 값 방어를 위해 1~50으로 강제
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)

        // 2) cursor 디코딩: 직전 페이지의 마지막 글 id. 없으면 null(첫 페이지). 깨진 값이면 InvalidTechPostCursorException(400)
        val cursorId = cursor?.let { TechPostCursor.decode(it) }

        // 3) 목록 조회: categoryId 있으면 EXISTS 필터 + id < cursorId, id desc, pageSize개.
        //    댓글 수는 tech_post_comment_count leftJoin + coalesce(0)로 함께 조회 (1:1이라 커서 영향 없음)
        val posts = postRepository.findPosts(categoryId, cursorId, pageSize)

        // 4) 작성자 닉네임 배치 조회 (N+1 회피): 글들의 userId를 모아 ums에 1번 질의 → uid→nickname Map
        //    UmsApiClient로 ums 경계를 추상화(MSA 분리 대비). 모놀리식은 users 직접 조회
        val nicknames = umsApiClient.findNicknames(posts.map { it.post.userId })

        // 5) 카테고리 배치 조회 (N+1 회피): postId들을 모아 1번 질의 후 postId→categoryId 리스트로 그룹핑
        //    글:카테고리는 1:N이라 목록 join 시 row가 불어나 limit이 깨짐 → 별도 IN 조회
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.post.id })
            .groupBy({ it.postId }, { it.categoryId })

        // 6) DTO 매핑: 4·5에서 만든 Map에서 닉네임·카테고리를 꺼내 TechPostSummaryResponse로 변환
        val data = posts.map { postWithCount ->
            val post = postWithCount.post
            TechPostSummaryResponse(
                id = post.id,
                author = TechPostDetailResponse.Author(uid = post.userId, nickname = nicknames[post.userId]),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = postWithCount.commentCount.toInt(),
                createdAt = post.createdAt,
            )
        }

        // 7) 다음 커서: 마지막(가장 과거) 글 id를 인코딩. 빈 결과면 null → 클라이언트는 빈 응답으로 끝을 판단
        val nextCursor = posts.lastOrNull()?.let { TechPostCursor.encode(it.post.id) }

        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    /**
     * 테크 피드 목록 조회 (offset 페이지네이션) — 학습용. 엔드포인트는 비활성(Resource 주석).
     */
    @Transactional(readOnly = true)
    fun getPostsByOffset(categoryId: Long?, page: Int, size: Int): ApiEnvelopPage<TechPostSummaryResponse> {
        val pageNumber = page.coerceAtLeast(0)
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val offset = pageNumber.toLong() * pageSize

        val totalCount = postRepository.countPosts(categoryId)
        val posts = postRepository.findPosts(categoryId, offset, pageSize)

        val nicknames = umsApiClient.findNicknames(posts.map { it.post.userId })
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.post.id })
            .groupBy({ it.postId }, { it.categoryId })

        val data = posts.map { postWithCount ->
            val post = postWithCount.post
            TechPostSummaryResponse(
                id = post.id,
                author = TechPostDetailResponse.Author(uid = post.userId, nickname = nicknames[post.userId]),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = postWithCount.commentCount.toInt(),
                createdAt = post.createdAt,
            )
        }

        val totalPages = if (totalCount == 0L) 0L else (totalCount + pageSize - 1) / pageSize
        return ApiEnvelopPage(
            data = data,
            page = pageNumber.toLong(),
            size = pageSize.toLong(),
            totalPages = totalPages,
            totalCount = totalCount,
            first = pageNumber == 0,
            last = totalPages == 0L || pageNumber.toLong() >= totalPages - 1,
        )
    }

    /**
     * 내 글 목록 조회 (cursor 페이지네이션) — 실사용. id desc 최신순 +
     * 피드(getPostsByCursor)와 동일 방식. 무한스크롤에 적합(일관성·인덱스 범위 스캔).
     * sectionPath는 구 API 호환용 쿼리 파라미터 — null 또는 "tech"만 허용.
     */
    @Transactional(readOnly = true)
    fun getMyPostsByCursor(userId: Long, sectionPath: String?, cursor: String?, size: Int): ApiEnvelopCursorPage<TechPostSummaryResponse> {
        validateSectionParam(sectionPath)
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val cursorId = cursor?.let { TechPostCursor.decode(it) }

        val posts = postRepository.findMyPosts(userId, cursorId, pageSize)

        val nickname = umsApiClient.findNickname(userId)
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.post.id })
            .groupBy({ it.postId }, { it.categoryId })

        val data = posts.map { postWithCount ->
            val post = postWithCount.post
            TechPostSummaryResponse(
                id = post.id,
                author = TechPostDetailResponse.Author(uid = post.userId, nickname = nickname),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = postWithCount.commentCount.toInt(),
                createdAt = post.createdAt,
            )
        }

        val nextCursor = posts.lastOrNull()?.let { TechPostCursor.encode(it.post.id) }
        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    /**
     * 내 글 목록 조회 (offset 페이지네이션) — 학습용. id desc + offset/limit + 전체 count.
     * cursor 방식(getMyPostsByCursor)과 대비하는 QueryDSL 학습 예제(offset+count).
     */
    @Transactional(readOnly = true)
    fun getMyPostsByOffset(userId: Long, sectionPath: String?, page: Int, size: Int): ApiEnvelopPage<TechPostSummaryResponse> {

        // 1) section 파라미터: 구 API 호환 검증 (null 또는 "tech"만 허용, 그 외 400)
        validateSectionParam(sectionPath)

        // 2) page/size 보정 후 offset 계산
        val pageNumber = page.coerceAtLeast(0)
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val offset = pageNumber.toLong() * pageSize

        // 3) 전체 개수 + 현재 페이지 목록 (동일 조건). count로 totalPages·last를 계산
        val totalCount = postRepository.countMyPosts(userId)
        val posts = postRepository.findMyPosts(userId, offset, pageSize)

        // 4) 작성자는 본인이라 닉네임은 단건 조회로 충분. 카테고리는 1:N이라 배치(IN) 조회
        val nickname = umsApiClient.findNickname(userId)
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.post.id })
            .groupBy({ it.postId }, { it.categoryId })

        // 5) DTO 매핑
        val data = posts.map { postWithCount ->
            val post = postWithCount.post
            TechPostSummaryResponse(
                id = post.id,
                author = TechPostDetailResponse.Author(uid = post.userId, nickname = nickname),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = postWithCount.commentCount.toInt(),
                createdAt = post.createdAt,
            )
        }

        // 6) 페이지 메타: totalPages = ceil(total/size). 빈 결과면 0페이지·last=true
        val totalPages = if (totalCount == 0L) 0L else (totalCount + pageSize - 1) / pageSize
        return ApiEnvelopPage(
            data = data,
            page = pageNumber.toLong(),
            size = pageSize.toLong(),
            totalPages = totalPages,
            totalCount = totalCount,
            first = pageNumber == 0,
            last = totalPages == 0L || pageNumber.toLong() >= totalPages - 1,
        )
    }
}
