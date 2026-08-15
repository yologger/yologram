from enum import Enum


class TechSearchSort(str, Enum):
    """
    검색 정렬 기준 — 프론트의 sort 파라미터·api-v1 enum과 같은 값. 게시글·뉴스가 공유한다.

    어느 쪽이든 2차 키를 둔다: 1차 키가 동점일 때 순서가 흔들리면
    페이징에서 같은 문서가 두 페이지에 나오거나 아예 빠진다.

    "최신"의 기준 시각은 대상마다 다르다 — 게시글은 작성 시각(createdAt),
    뉴스는 발행 시각(publishedAt)이다(목록 API 정렬과 같은 기준을 쓴다).
    """

    RELEVANCE = "RELEVANCE"
    LATEST = "LATEST"
