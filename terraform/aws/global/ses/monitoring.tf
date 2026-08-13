# CloudWatch + SNS 모니터링 (SES 발송량/이상 감시)
#
# 목적: 발송량 급증, Bounce/Complaint율 상승을 조기 감지 (요금 사고/계정 정지 예방)
# 비용: CloudWatch 알람 1개(프리티어 10개 내) + SNS 이메일(월 1,000건 무료) → 사실상 $0
#
# 사용 방법:
#   1. 아래 블록 주석(/* */)을 해제
#   2. alarm_email 기본값을 실제 수신 주소로 변경
#   3. terraform apply
#   4. SNS 구독 확인 메일이 오면 수락(Confirm)해야 알림 수신

/*
variable "alarm_email" {
  description = "SES 알람 수신 이메일"
  type        = string
  default     = "alerts@yologram.link"
}

resource "aws_sns_topic" "ses_alerts" {
  name = "ses-alerts"
}

resource "aws_sns_topic_subscription" "ses_alerts_email" {
  topic_arn = aws_sns_topic.ses_alerts.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

# 일 발송량 100건 초과 알람
resource "aws_cloudwatch_metric_alarm" "ses_daily_send" {
  alarm_name          = "ses-daily-send-over-100"
  alarm_description   = "SES 일 발송량 100건 초과"
  namespace           = "AWS/SES"
  metric_name         = "Send"
  statistic           = "Sum"
  period              = 86400
  evaluation_periods  = 1
  comparison_operator = "GreaterThanThreshold"
  threshold           = 100
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.ses_alerts.arn]
}
*/
