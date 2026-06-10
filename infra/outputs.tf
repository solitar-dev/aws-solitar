output "vpc_id" {
  description = "VPC id (consumed by phase-04 security groups / ECS / ALB)."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnet ids for the ALB and Fargate tasks."
  value       = aws_subnet.public[*].id
}

output "public_route_table_id" {
  description = "Public route table id."
  value       = aws_route_table.public.id
}

output "dynamodb_table_names" {
  description = "DynamoDB table names."
  value       = { urls = aws_dynamodb_table.urls.name, statistics = aws_dynamodb_table.statistics.name }
}

output "dynamodb_table_arns" {
  description = "DynamoDB table ARNs."
  value       = { urls = aws_dynamodb_table.urls.arn, statistics = aws_dynamodb_table.statistics.arn }
}

output "sqs_queue_urls" {
  description = "SQS queue URLs."
  value = {
    link_created   = aws_sqs_queue.link_created.id
    link_forwarded = aws_sqs_queue.link_forwarded.id
    dlq            = aws_sqs_queue.dlq.id
  }
}

output "sqs_queue_arns" {
  description = "SQS queue ARNs."
  value = {
    link_created   = aws_sqs_queue.link_created.arn
    link_forwarded = aws_sqs_queue.link_forwarded.arn
    dlq            = aws_sqs_queue.dlq.arn
  }
}

output "ecr_repository_url" {
  description = "ECR repository URL for image push/pull."
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_task_role_arn" {
  description = "ECS task role ARN (app runtime identity)."
  value       = aws_iam_role.ecs_task.arn
}

output "ecs_execution_role_arn" {
  description = "ECS execution role ARN (image pull + logs)."
  value       = aws_iam_role.ecs_execution.arn
}

output "github_oidc_role_arn" {
  description = "GitHub Actions OIDC role ARN (consumed by phase-06 CI/CD)."
  value       = aws_iam_role.github_oidc.arn
}

output "frontend_bucket_name" {
  description = "Frontend S3 bucket name phase-04 must create (the CI/CD role is scoped to it)."
  value       = local.frontend_bucket_name
}

# --- phase-04 edge/compute (consumed by P5 deploy + P6 CI) ---

output "route53_zone_id" {
  description = "Route 53 hosted zone id."
  value       = aws_route53_zone.main.zone_id
}

output "route53_nameservers" {
  description = "Set these as the domain's nameservers at the registrar (DNS cutover, before ACM validation)."
  value       = aws_route53_zone.main.name_servers
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution id (for cache invalidations in P5/P6)."
  value       = aws_cloudfront_distribution.main.id
}

output "cloudfront_domain_name" {
  description = "CloudFront distribution domain name."
  value       = aws_cloudfront_distribution.main.domain_name
}

output "alb_dns_name" {
  description = "ALB DNS name (origin behind alb.<domain>)."
  value       = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  description = "ECS cluster name (for CI deploys)."
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name (for CI deploys)."
  value       = aws_ecs_service.app.name
}
