package link.yologram.api.v1.domain.ums.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "user_password_reset_code")
@EntityListeners(AuditingEntityListener::class)
class UserPasswordResetCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 200)
    val email: String,

    @Column(nullable = false, length = 6)
    val code: String,

    @Column(nullable = false)
    var verified: Boolean = false,

    @Column(nullable = false)
    val expiredAt: LocalDateTime,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
