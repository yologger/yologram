output "cluster_name" {
  value = aws_ecs_cluster.prod.name
}

output "cluster_arn" {
  value = aws_ecs_cluster.prod.arn
}

output "task_execution_role_arn" {
  value = aws_iam_role.ecs_task_execution.arn
}
