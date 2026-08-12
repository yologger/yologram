package link.yologram.api.v1.domain.ums.resolver

/**
 * 선택 인증 파라미터 주입 — 공개 API지만 로그인 시 개인화 값(likedByMe 등)을 채우는 곳에서 사용.
 * Authorization 헤더가 없으면 null(비로그인 취급), 있으면 정상 검증(형식 불량·무효 토큰은 401).
 * "틀린 토큰인데 비로그인으로 조용히 처리"는 클라이언트 버그(만료 토큰 방치)를 숨기므로 하지 않는다.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class OptionalAuthenticatedUser
