from app.config.settings import get_settings
from app.domain.ums.email_sender import EmailSender, SesEmailSender, StubEmailSender


def get_email_sender() -> EmailSender:
    settings = get_settings()
    if settings.app_profile == "prod":
        return SesEmailSender()
    return StubEmailSender()
