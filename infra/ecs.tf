resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.project}"
  retention_in_days = 14

  tags = { Name = "${var.project}-logs" }
}

resource "aws_ecs_cluster" "main" {
  name = var.project

  setting {
    name  = "containerInsights"
    value = "disabled" # keep cost at zero
  }

  tags = { Name = "${var.project}-cluster" }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]
}

resource "aws_ecs_task_definition" "app" {
  family                   = var.project
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }

  container_definitions = jsonencode([{
    name         = var.project
    image        = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"
    essential    = true
    portMappings = [{ containerPort = var.container_port, protocol = "tcp" }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "AWS_REGION", value = var.region },
      { name = "BACKEND_SERVER_PORT", value = tostring(var.container_port) },
      { name = "VALKEY_HOST", value = aws_elasticache_serverless_cache.valkey.endpoint[0].address },
      { name = "VALKEY_SSL", value = "true" },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "app"
      }
    }
  }])

  tags = { Name = "${var.project}-task" }
}

resource "aws_ecs_service" "app" {
  name            = var.project
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1

  # Single task on Spot for cost (~$2-3/mo). Swap to FARGATE (or add a base=1 FARGATE entry) if you
  # prefer fewer ~2-min interruptions over the Spot discount.
  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
  }

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.task.id]
    assign_public_ip = true # required for ECR/SQS reachability without a NAT gateway
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = var.project
    container_port   = var.container_port
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_minimum_healthy_percent = 0 # single task: allow a brief gap on deploy
  deployment_maximum_percent         = 100
  health_check_grace_period_seconds  = 60 # GraalVM native boot headroom

  depends_on = [aws_lb_listener.https]

  # CI/CD (phase-06) registers new task-def revisions with updated images; don't let TF revert that.
  lifecycle {
    ignore_changes = [task_definition]
  }

  tags = { Name = "${var.project}-service" }
}
