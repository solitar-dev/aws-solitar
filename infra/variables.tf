variable "region" {
  description = "Primary AWS region for the stack."
  type        = string
  default     = "ap-southeast-1"
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

variable "health_check_path" {
  description = "ALB target-group health check path. Backend GET / returns 200 (no Actuator)."
  type        = string
  default     = "/"
}

variable "image_tag" {
  description = "Container image tag the ECS task definition references. CI overwrites the running task def; TF keeps this as the bootstrap default."
  type        = string
  default     = "latest"
}
