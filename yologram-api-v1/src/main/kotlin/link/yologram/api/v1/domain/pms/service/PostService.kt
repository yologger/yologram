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
     * 섹션 피드 목록 조회 (공개). id desc 최신순 + cursor(keyset) 페이지네이션.
     * 정렬/조회는 repository(QueryDSL)가, 페이지 경계·DTO 매핑·커서 생성은 여기서 담당.
     */
    @Transactional(readOnly = true)
    fun getPosts(sectionPath: String, categoryId: Long?, cursor: String?, size: Int): ApiEnvelopCursorPage<PostSummaryResponse> {

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
}
