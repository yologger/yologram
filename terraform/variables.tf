variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "instance_name" {
  description = "Lightsail instance name"
  type        = string
  default     = "yologger-n8n"
}

variable "bundle_id" {
  description = "Lightsail instance bundle (nano_3_0: $3.5/mo, micro_3_0: $5/mo, small_3_0: $10/mo)"
  type        = string
  default     = "micro_3_0"
}

variable "blueprint_id" {
  description = "Lightsail instance OS blueprint"
  type        = string
  default     = "amazon_linux_2023"
}
