package link.yologram.api.v1.domain.ums.entity

import jakarta.persistence.*
import link.yologram.api.v1.domain.ums.enum.UserStatus
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "admin_user")
@EntityListeners(AuditingEntityListener::class)
class AdminUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 200)
    val email: String,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(nullable = false, length = 200)
    var password: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var joinedDate: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now(),
)
