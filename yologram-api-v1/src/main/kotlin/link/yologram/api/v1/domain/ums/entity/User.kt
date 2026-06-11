package link.yologram.api.v1.domain.ums.entity

import jakarta.persistence.*
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.enum.UserType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 200)
    val email: String,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(nullable = false, length = 200)
    var nickname: String,

    @Column(nullable = false, length = 200)
    var password: String,

    @Column(length = 512)
    var avatar: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: UserType = UserType.DEFAULT,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    var deletedDate: LocalDateTime? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var joinedDate: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now(),
)
