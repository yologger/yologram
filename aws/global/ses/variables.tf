variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "domain" {
  type    = string
  default = "yologram.link"
}

variable "mail_from_subdomain" {
  description = "Custom MAIL FROM 서브도메인 (Return-Path 용)"
  type        = string
  default     = "mail"
}
