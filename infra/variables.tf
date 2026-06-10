variable "region" {
  description = "Primary AWS region for the stack."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Project name; used for resource naming and the Project cost-allocation tag."
  type        = string
  default     = "solitar"
}

variable "domain_name" {
  description = "Apex domain served by CloudFront (consumed in phase-04)."
  type        = string
  default     = "solitar.link"
}

variable "alert_email" {
  description = "Email address that receives the AWS Budgets alarm (requires one-time confirmation)."
  type        = string
  default     = "lovelyloco805@gmail.com"
}

variable "github_repo" {
  description = "GitHub repository 'owner/name' trusted by the CI/CD OIDC role (e.g. acme/solitar). Required: set in terraform.tfvars so the OIDC trust is never scoped to the wrong repo."
  type        = string

  validation {
    condition     = can(regex("^[^/]+/[^/]+$", var.github_repo))
    error_message = "github_repo must be in 'owner/name' form, e.g. solitar-dev/aws-solitar."
  }
}

variable "container_port" {
  description = "Backend container listen port (consumed in phase-04)."
  type        = number
  default     = 8080
}
