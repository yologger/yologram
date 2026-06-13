package link.yologram.api.v1.domain.ums.service

interface EmailSender {
    fun sendVerificationCode(to: String, code: String)
    fun sendPasswordResetCode(to: String, code: String)
}
