output "search_indexing_queue_url" {
  description = "검색 인덱싱 큐 URL (api-v1·v2 발행 / worker 소비)"
  value       = aws_sqs_queue.search_indexing_prod.url
}

output "search_indexing_queue_arn" {
  description = "IAM 정책에서 참조할 큐 ARN"
  value       = aws_sqs_queue.search_indexing_prod.arn
}

output "search_indexing_dlq_url" {
  description = "실패 메시지 격리 큐 URL"
  value       = aws_sqs_queue.search_indexing_dlq_prod.url
}
