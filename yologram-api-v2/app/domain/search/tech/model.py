from enum import Enum


class TechPostSearchSort(str, Enum):
    """
    검색 정렬 기준 — 프론트의 sort 파라미터·api-v1 enum과 같은 값.

    어느 쪽이든 2차 키를 둔다: 1차 키가 동점일 때 순서가 흔들리면
    페이징에서 같은 문서가 두 페이지에 나오거나 아예 빠진다.
    """

    RELEVANCE = "RELEVANCE"
    LATEST = "LATEST"
