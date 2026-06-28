package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.pms.entity.Post
import link.yologram.api.v1.domain.pms.entity.PostCategoryMapping
import link.yologram.api.v1.domain.pms.exception.InvalidPostCategoryException
import link.yologram.api.v1.domain.pms.exception.PostNotFoundException
import link.yologram.api.v1.domain.pms.model.CreatePostRequest
import link.yologram.api.v1.domain.pms.model.CreatePostResponse
import link.yologram.api.v1.domain.pms.model.PostCursor
import link.yologram.api.v1.domain.pms.model.PostDetailResponse
import link.yologram.api.v1.domain.pms.model.PostSummaryResponse
import link.yologram.api.v1.domain.pms.repository.PostCategoryMappingRepository
import link.yologram.api.v1.domain.pms.repository.PostRepository
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postCategoryMappingRepository: PostCategoryMappingRepository,
    private val categoryQueryClient: PostCategoryQueryClient,
    private val userQueryClient: UserQueryClient,
) {

    // 게시글 단건 생성
    @Transactional
    fun create(sectionPath: String, userId: Long, request: CreatePostRequest): CreatePostResponse {
        val section = Section.fromPath(sectionPath)
        val categoryIds = request.categoryIds.toSet()

        if (!categoryQueryClient.allActiveInSection(section, categoryIds)) {
            throw InvalidPostCategoryException()
        }

        val post = postRepository.save(
            Post(
                section = section,
                userId = userId,
                title = request.title?.takeIf { it.isNotBlank() },
                content = request.content!!,
            )
        )

        categoryIds.forEach { categoryId ->
            postCategoryMappingRepository.save(PostCategoryMapping(postId = post.id, categoryId = categoryId))
        }

        return CreatePostResponse(id = post.id)
    }

    // 게시글 단건 조회
    @Transactional(readOnly = true)
    fun getPost(sectionPath: String, id: Long): PostDetailResponse {
        val section = Section.fromPath(sectionPath)
        val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException()
        if (post.section != section) throw PostNotFoundException()

        val categoryIds = postCategoryMappingRepository.findByPostId(post.id).map { it.categoryId }
        val nickname = userQueryClient.findNickname(post.userId)

        return PostDetailResponse(
            id = post.id,
            section = post.section,
            author = PostDetailResponse.Author(uid = post.userId, nickname = nickname),
            title = post.title,
            content = post.content,
            categoryIds = categoryIds,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            createdAt = post.createdAt,
        )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    /**
     * 섹션 피드 목록 조회 (cursor 페이지네이션)
     */
    @Transactional(readOnly = true)
    fun getPostsByCursor(sectionPath: String, categoryId: Long?, cursor: String?, size: Int): ApiEnvelopCursorPage<PostSummaryResponse> {

        // 1) 섹션 검증: "tech" 같은 경로 문자열 → Section enum. 잘못된 값이면 InvalidSectionException(400)
        val section = Section.fromPath(sectionPath)

        // 2) size 보정: 0·음수·과도한 값 방어를 위해 1~50으로 강제
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)

        // 3) cursor 디코딩: 직전 페이지의 마지막 글 id. 없으면 null(첫 페이지). 깨진 값이면 InvalidCursorException(400)
        val cursorId = cursor?.let { PostCursor.decode(it) }

        // 4) 목록 조회: section 필터 (+categoryId 있으면 EXISTS 필터) + id < cursorId, id desc, pageSize개
        val posts = postRepository.findPostsBySection(section, categoryId, cursorId, pageSize)

        // 5) 작성자 닉네임 배치 조회 (N+1 회피): 글들의 userId를 모아 ums에 1번 질의 → uid→nickname Map
        //    UserQueryClient로 ums 경계를 추상화(MSA 분리 대비). 모놀리식은 users 직접 조회
        val nicknames = userQueryClient.findNicknames(posts.map { it.userId })

        // 6) 카테고리 배치 조회 (N+1 회피): postId들을 모아 1번 질의 후 postId→categoryId 리스트로 그룹핑
        //    글:카테고리는 1:N이라 목록 join 시 row가 불어나 limit이 깨짐 → 별도 IN 조회
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.id })
            .groupBy({ it.postId }, { it.categoryId })

        // 7) DTO 매핑: 5·6에서 만든 Map에서 닉네임·카테고리를 꺼내 PostSummaryResponse로 변환
        val data = posts.map { post ->
            PostSummaryResponse(
                id = post.id,
                section = post.section,
                author = PostDetailResponse.Author(uid = post.userId, nickname = nicknames[post.userId]),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                createdAt = post.createdAt,
            )
        }

        // 8) 다음 커서: 마지막(가장 과거) 글 id를 인코딩. 빈 결과면 null → 클라이언트는 빈 응답으로 끝을 판단
        val nextCursor = posts.lastOrNull()?.let { PostCursor.encode(it.id) }

        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    /**
     * 섹션 피드 목록 조회 (offset 페이지네이션)
     */
    @Transactional(readOnly = true)
    fun getPostsByOffset(sectionPath: String, categoryId: Long?, page: Int, size: Int): ApiEnvelopPage<PostSummaryResponse> {
        val section = Section.fromPath(sectionPath)
        val pageNumber = page.coerceAtLeast(0)
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val offset = pageNumber.toLong() * pageSize

        val totalCount = postRepository.countPostsBySection(section, categoryId)
        val posts = postRepository.findPostsBySection(section, categoryId, offset, pageSize)

        val nicknames = userQueryClient.findNicknames(posts.map { it.userId })
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.id })
            .groupBy({ it.postId }, { it.categoryId })

        val data = posts.map { post ->
            PostSummaryResponse(
                id = post.id,
                section = post.section,
                author = PostDetailResponse.Author(uid = post.userId, nickname = nicknames[post.userId]),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = post.commentCount,
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
     * 내 글 목록 조회 (cursor 페이지네이션.) — 실사용. id desc 최신순 +
     * 피드(getPostsByCursor)와 동일 방식. 무한스크롤에 적합(일관성·인덱스 범위 스캔).
     */
    @Transactional(readOnly = true)
    fun getMyPostsByCursor(userId: Long, sectionPath: String?, cursor: String?, size: Int): ApiEnvelopCursorPage<PostSummaryResponse> {
        val section = sectionPath?.let { Section.fromPath(it) }
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val cursorId = cursor?.let { PostCursor.decode(it) }

        val posts = postRepository.findMyPosts(userId, section, cursorId, pageSize)

        val nickname = userQueryClient.findNickname(userId)
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.id })
            .groupBy({ it.postId }, { it.categoryId })

        val data = posts.map { post ->
            PostSummaryResponse(
                id = post.id,
                section = post.section,
                author = PostDetailResponse.Author(uid = post.userId, nickname = nickname),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                createdAt = post.createdAt,
            )
        }

        val nextCursor = posts.lastOrNull()?.let { PostCursor.encode(it.id) }
        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    /**
     * 내 글 목록 조회 (offset 페이지네이션) — 학습용. id desc + offset/limit + 전체 count.
     * cursor 방식(getMyPosts)과 대비하는 QueryDSL 학습 예제(offset+count+동적조건).
     */
    @Transactional(readOnly = true)
    fun getMyPostsByOffset(userId: Long, sectionPath: String?, page: Int, size: Int): ApiEnvelopPage<PostSummaryResponse> {

        // 1) 섹션: 지정되면 검증(잘못되면 InvalidSectionException 400), 생략되면 전체(null)
        val section = sectionPath?.let { Section.fromPath(it) }

        // 2) page/size 보정 후 offset 계산
        val pageNumber = page.coerceAtLeast(0)
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val offset = pageNumber.toLong() * pageSize

        // 3) 전체 개수 + 현재 페이지 목록 (동일 조건). count로 totalPages·last를 계산
        val totalCount = postRepository.countMyPosts(userId, section)
        val posts = postRepository.findMyPosts(userId, section, offset, pageSize)

        // 4) 작성자는 본인이라 닉네임은 단건 조회로 충분. 카테고리는 1:N이라 배치(IN) 조회
        val nickname = userQueryClient.findNickname(userId)
        val categoryIdsByPost = postCategoryMappingRepository.findByPostIdIn(posts.map { it.id })
            .groupBy({ it.postId }, { it.categoryId })

        // 5) DTO 매핑
        val data = posts.map { post ->
            PostSummaryResponse(
                id = post.id,
                section = post.section,
                author = PostDetailResponse.Author(uid = post.userId, nickname = nickname),
                title = post.title,
                content = post.content,
                categoryIds = categoryIdsByPost[post.id] ?: emptyList(),
                likeCount = post.likeCount,
                commentCount = post.commentCount,
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
