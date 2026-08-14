package link.yologram.api.v1.domain.search.exception

open class SearchException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidIndexRangeException : SearchException("인덱싱 범위가 유효하지 않습니다.", "INVALID_INDEX_RANGE")

class BlankSearchKeywordException : SearchException("검색어를 입력해주세요.", "BLANK_SEARCH_KEYWORD")

/**
 * OpenSearch의 max_result_window(기본 10000) 초과 — from + size가 그 값을 넘으면 엔진이 예외를 낸다.
 * 막지 않으면 전역 폴백에서 500이 되므로 400으로 돌려준다(요청이 잘못된 것이지 서버 오류가 아니다).
 */
class SearchPageTooDeepException : SearchException("더 이상 조회할 수 없는 페이지입니다.", "SEARCH_PAGE_TOO_DEEP")
