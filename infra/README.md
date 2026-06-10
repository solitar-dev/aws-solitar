# Solitar Infrastructure (Terraform)

Foundational AWS substrate (phase-03): VPC, DynamoDB, SQS, ECR, IAM, budget. Compute + edge
(ECS/ALB/CloudFront/Route53/S3) are added in phase-04.

## Design constraints

- **No NAT gateway.** Fargate tasks run in public subnets with a public IP and pull from ECR over
  the IGW. NAT would be ~$32/mo — the single biggest free-plan cost trap.
- **One free VPC endpoint** (DynamoDB gateway). SQS is reached over the public IP (no free SQS
  endpoint exists).
- **Single Fargate task, on-demand DynamoDB** — stays inside the always-free tier.
- **`force_delete`/`force_destroy`** are set on ECR (and S3 in phase-04) so `terraform destroy`
  never strands on a non-empty repo/bucket. The credit-funded account is meant to be torn down.

## Prerequisites

- Terraform >= 1.10, AWS credentials with admin-ish bootstrap rights for the target account.
- `cp terraform.tfvars.example terraform.tfvars` and set **`github_repo`** (required, `owner/name`).

## Apply order

```bash
terraform init
terraform fmt -check
terraform validate
terraform plan -out tfplan
terraform apply tfplan
```

The foundation is free / near-zero cost, so applying it early to confirm credentials + region is
safe. **Do not** apply phase-04 edge resources until the phase-02 native image is proven to boot.

## After apply

- Confirm the **budget alarm email** (AWS sends a one-time subscription confirmation).
- Capture outputs for later phases: `terraform output -json > outputs.json`.
  Phase-04 needs the VPC/subnets, ECR URL, role ARNs, queue/table names, and `frontend_bucket_name`.
  Phase-06 needs `github_oidc_role_arn` and `ecr_repository_url`.

## Teardown

```bash
terraform destroy
```

State is local (`terraform.tfstate`, gitignored). Back it up before destroy if you want a record.
