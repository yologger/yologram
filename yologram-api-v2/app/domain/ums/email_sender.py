import logging
from typing import Protocol

import boto3

from app.config.settings import get_settings

logger = logging.getLogger(__name__)


class EmailSender(Protocol):
    def send_verification_code(self, to: str, code: str) -> None: ...
    def send_password_reset_code(self, to: str, code: str) -> None: ...


class StubEmailSender:
    def send_verification_code(self, to: str, code: str) -> None:
        logger.info(f"[StubEmailSender] to={to}, code={code}")

    def send_password_reset_code(self, to: str, code: str) -> None:
        logger.info(f"[StubEmailSender] 비밀번호 재설정 to={to}, code={code}")


class SesEmailSender:
    def __init__(self):
        self.client = boto3.client("ses", region_name="ap-northeast-2")
        self.from_address = get_settings().ses_from_address

    def send_verification_code(self, to: str, code: str) -> None:
        html = f"""<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="margin:0;padding:0;background-color:#f4f4f5;font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif">
  <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 0">
    <tr><td align="center">
      <table width="420" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,0.06);padding:40px">
        <tr><td style="text-align:center;padding-bottom:24px">
          <span style="font-size:22px;font-weight:700;color:#18181b">yologram</span>
        </td></tr>
        <tr><td style="text-align:center;padding-bottom:16px">
          <span style="font-size:16px;color:#3f3f46">이메일 인증 코드</span>
        </td></tr>
        <tr><td style="text-align:center;padding-bottom:24px">
          <span style="font-size:32px;font-weight:700;letter-spacing:6px;color:#2563eb">{code}</span>
        </td></tr>
        <tr><td style="text-align:center;padding-bottom:8px">
          <span style="font-size:13px;color:#71717a">5분 이내에 입력해주세요.</span>
        </td></tr>
        <tr><td style="text-align:center;border-top:1px solid #e4e4e7;padding-top:20px">
          <span style="font-size:12px;color:#a1a1aa">본 메일은 yologram 회원가입을 위해 발송되었습니다.</span>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>"""

        try:
            self.client.send_email(
                Source=self.from_address,
                Destination={"ToAddresses": [to]},
                Message={
                    "Subject": {"Data": "[yologram] 이메일 인증 코드", "Charset": "UTF-8"},
                    "Body": {"Html": {"Data": html, "Charset": "UTF-8"}},
                },
            )
            logger.info(f"[SesEmailSender] to={to}")
        except Exception as e:
            logger.error(f"[SesEmailSender] 발송 실패: to={to}", exc_info=True)
            raise e

    def send_password_reset_code(self, to: str, code: str) -> None:
        html = f"""<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="margin:0;padding:0;background-color:#f4f4f5;font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif">
  <table width="100%" cellpadding="0" cellspacing="0" style="padding:40px 0">
    <tr><td align="center">
      <table width="420" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,0.06);padding:40px">
        <tr><td style="text-align:center;padding-bottom:24px">
          <span style="font-size:22px;font-weight:700;color:#18181b">yologram</span>
        </td></tr>
        <tr><td style="text-align:center;padding-bottom:16px">
          <span style="font-size:16px;color:#3f3f46">비밀번호 재설정 코드</span>
        </td></tr>
        <tr><td style="text-align:center;padding-bottom:24px">
          <span style="font-size:32px;font-weight:700;letter-spacing:6px;color:#2563eb">{code}</span>
        </td></tr>
        <tr><td style="text-align:center;padding-bottom:8px">
          <span style="font-size:13px;color:#71717a">5분 이내에 입력해주세요. 본인이 요청하지 않았다면 무시하세요.</span>
        </td></tr>
        <tr><td style="text-align:center;border-top:1px solid #e4e4e7;padding-top:20px">
          <span style="font-size:12px;color:#a1a1aa">본 메일은 yologram 비밀번호 재설정을 위해 발송되었습니다.</span>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>"""

        try:
            self.client.send_email(
                Source=self.from_address,
                Destination={"ToAddresses": [to]},
                Message={
                    "Subject": {"Data": "[yologram] 비밀번호 재설정 코드", "Charset": "UTF-8"},
                    "Body": {"Html": {"Data": html, "Charset": "UTF-8"}},
                },
            )
            logger.info(f"[SesEmailSender] 비밀번호 재설정 to={to}")
        except Exception as e:
            logger.error(f"[SesEmailSender] 비밀번호 재설정 발송 실패: to={to}", exc_info=True)
            raise e
