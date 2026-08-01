package link.yologram.api.v1.domain.ums.enum

/**
 * 어드민 역할 — DB는 ENUM('ADMIN','OWNER') (prod hbm2ddl=validate).
 * 선언 순서를 DDL 값 순서와 동일하게 유지할 것 (Hibernate enum DDL 생성 순서 일치).
 */
enum class AdminUserRole {
    ADMIN,
    OWNER,
}
