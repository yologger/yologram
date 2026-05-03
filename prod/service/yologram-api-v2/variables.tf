variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "vpc_link_id" {
  description = "API Gateway VPC Link ID"
  type        = string
}
