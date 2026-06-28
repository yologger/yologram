locals {
  mail_from_domain = "${var.mail_from_subdomain}.${var.domain}"
}

#####################
## Domain Identity ##
#####################
resource "aws_ses_domain_identity" "this" {
  domain = var.domain
}

# 도메인 소유권 검증 TXT (_amazonses.<domain>)
resource "aws_route53_record" "verification" {
  zone_id = data.aws_route53_zone.this.zone_id
  name    = "_amazonses.${var.domain}"
  type    = "TXT"
  ttl     = 600
  records = [aws_ses_domain_identity.this.verification_token]
}

resource "aws_ses_domain_identity_verification" "this" {
  domain     = aws_ses_domain_identity.this.id
  depends_on = [aws_route53_record.verification]
}

##########
## DKIM ##
##########
resource "aws_ses_domain_dkim" "this" {
  domain = aws_ses_domain_identity.this.domain
}

# DKIM CNAME 3개 (<token>._domainkey.<domain> → <token>.dkim.amazonses.com)
resource "aws_route53_record" "dkim" {
  count   = 3
  zone_id = data.aws_route53_zone.this.zone_id
  name    = "${aws_ses_domain_dkim.this.dkim_tokens[count.index]}._domainkey.${var.domain}"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.this.dkim_tokens[count.index]}.dkim.amazonses.com"]
}

###############
## MAIL FROM ##
###############
resource "aws_ses_domain_mail_from" "this" {
  domain           = aws_ses_domain_identity.this.domain
  mail_from_domain = local.mail_from_domain
}

# 반송 피드백 경로 (MX)
resource "aws_route53_record" "mail_from_mx" {
  zone_id = data.aws_route53_zone.this.zone_id
  name    = aws_ses_domain_mail_from.this.mail_from_domain
  type    = "MX"
  ttl     = 600
  records = ["10 feedback-smtp.${var.aws_region}.amazonses.com"]
}

# SPF (TXT)
resource "aws_route53_record" "mail_from_spf" {
  zone_id = data.aws_route53_zone.this.zone_id
  name    = aws_ses_domain_mail_from.this.mail_from_domain
  type    = "TXT"
  ttl     = 600
  records = ["v=spf1 include:amazonses.com ~all"]
}

###########
## DMARC ##
###########
resource "aws_route53_record" "dmarc" {
  zone_id = data.aws_route53_zone.this.zone_id
  name    = "_dmarc.${var.domain}"
  type    = "TXT"
  ttl     = 600
  records = ["v=DMARC1; p=none;"]
}
