variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "route53_zone_id" {
  description = "Route 53 hosted zone ID for yologram.link"
  type        = string
  default     = "Z0365311288PJQTGLT9S3"
}

