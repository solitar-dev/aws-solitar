data "aws_caller_identity" "current" {}

locals {
  # Frontend bucket name is derived here (account-unique) and consumed by phase-04, which MUST create
  # the bucket with exactly this name so the CI/CD role's S3 permissions line up.
  frontend_bucket_name = "${var.project}-frontend-${data.aws_caller_identity.current.account_id}"
}

# ---------------------------------------------------------------------------
# ECS task role — the application's runtime identity (DynamoDB + SQS + logs).
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_task" {
  name               = "${var.project}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

data "aws_iam_policy_document" "task_app" {
  statement {
    sid       = "DynamoDbData"
    actions   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem"]
    resources = [aws_dynamodb_table.urls.arn, aws_dynamodb_table.statistics.arn]
  }

  statement {
    sid = "SqsAccess"
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
    ]
    resources = [
      aws_sqs_queue.link_created.arn,
      aws_sqs_queue.link_forwarded.arn,
      aws_sqs_queue.dlq.arn,
    ]
  }

  statement {
    sid       = "Logs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["arn:aws:logs:${var.region}:${data.aws_caller_identity.current.account_id}:log-group:/ecs/${var.project}*"]
  }
}

resource "aws_iam_role_policy" "task_app" {
  name   = "${var.project}-task-app"
  role   = aws_iam_role.ecs_task.id
  policy = data.aws_iam_policy_document.task_app.json
}

# ---------------------------------------------------------------------------
# ECS execution role — pulls the image and ships logs (separate from app data access).
# ---------------------------------------------------------------------------
resource "aws_iam_role" "ecs_execution" {
  name               = "${var.project}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ---------------------------------------------------------------------------
# GitHub Actions OIDC — short-lived STS instead of long-lived IAM user keys.
# ---------------------------------------------------------------------------
data "tls_certificate" "github" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github.certificates[0].sha1_fingerprint]
}

data "aws_iam_policy_document" "github_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repo}:*"]
    }
  }
}

resource "aws_iam_role" "github_oidc" {
  name               = "${var.project}-github-oidc"
  assume_role_policy = data.aws_iam_policy_document.github_assume.json
}

data "aws_iam_policy_document" "github_perms" {
  statement {
    sid       = "EcrAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
    ]
    resources = [aws_ecr_repository.app.arn]
  }

  # ECS task-definition registration and service description don't support resource-level scoping.
  statement {
    sid       = "EcsDeploy"
    actions   = ["ecs:RegisterTaskDefinition", "ecs:DescribeTaskDefinition", "ecs:UpdateService", "ecs:DescribeServices"]
    resources = ["*"]
  }

  statement {
    sid       = "PassRoles"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.ecs_task.arn, aws_iam_role.ecs_execution.arn]
  }

  statement {
    sid       = "FrontendSync"
    actions   = ["s3:ListBucket", "s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["arn:aws:s3:::${local.frontend_bucket_name}", "arn:aws:s3:::${local.frontend_bucket_name}/*"]
  }

  # CreateInvalidation has no resource-level permissions; scope is the whole account.
  statement {
    sid       = "CloudFrontInvalidate"
    actions   = ["cloudfront:CreateInvalidation"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_oidc" {
  name   = "${var.project}-github-oidc"
  role   = aws_iam_role.github_oidc.id
  policy = data.aws_iam_policy_document.github_perms.json
}
