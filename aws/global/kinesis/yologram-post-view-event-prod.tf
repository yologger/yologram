resource "aws_kinesis_stream" "yologram_post_view_event_prod" {
  name             = "yologram-post-view-event-prod"
  shard_count      = 1
  retention_period = 24

  stream_mode_details {
    stream_mode = "PROVISIONED"
  }

  tags = {
    Name = "yologram-post-view-event-prod"
  }
}

output "yologram_post_view_event_prod_arn" {
  value = aws_kinesis_stream.yologram_post_view_event_prod.arn
}
