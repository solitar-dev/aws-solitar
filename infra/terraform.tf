terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Local state is fine for a solo hobby project. To move to S3 later (TF >= 1.10 has a native
  # lockfile, no DynamoDB lock table needed), uncomment and fill in:
  # backend "s3" {
  #   bucket       = "solitar-tfstate-<account-id>"
  #   key          = "infra/terraform.tfstate"
  #   region       = "us-east-1"
  #   use_lockfile = true
  # }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project = var.project
    }
  }
}

# ACM certificates consumed by CloudFront MUST live in us-east-1. Declared here so phase-04 can
# reference `provider = aws.us_east_1` without re-declaring it.
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"

  default_tags {
    tags = {
      Project = var.project
    }
  }
}
