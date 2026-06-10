# Deployment Guide

How Solitar is built, deployed, and operated on AWS. Architecture lives in
[system-architecture.md](./system-architecture.md); teardown in
[teardown-and-upgrade-runbook.md](./teardown-and-upgrade-runbook.md).

## Stack

- **Backend** — Spring Boot 4 (Kotlin), GraalVM native image (ARM64), ECS Fargate Spot behind an ALB.
- **Frontend** — Nuxt 4 static export (`nuxt generate`) on S3 + CloudFront.
- **Data** — DynamoDB (`urls`, `statistics`), SQS (`link-created`, `link-forwarded`, `link-events-dlq`).
- **Infra** — Terraform in [`infra/`](../infra) (provider AWS `~> 6`).

## One-time bootstrap

1. **Apply the foundation + edge** — follow [`infra/README.md`](../infra/README.md). Set `github_repo`
   in `terraform.tfvars`. The DNS cutover (Route 53 NS at the registrar BEFORE ACM validation) is
   sequenced in the phase-04 runbook — get the certs validated before applying the rest.
2. **First image** — the ECS task definition references `:latest`; push one image so the service can
   start (the backend workflow does this on the first run, or build/push manually).
3. **GitHub repo variables** (Settings → Variables → Actions; non-secret, OIDC means no keys):

    | Variable                     | Source (`terraform output`)                       |
    | ---------------------------- | ------------------------------------------------- |
    | `AWS_OIDC_ROLE_ARN`          | `github_oidc_role_arn`                            |
    | `ECR_REPOSITORY`             | repo name (`solitar`) / from `ecr_repository_url` |
    | `ECS_CLUSTER`                | `ecs_cluster_name`                                |
    | `ECS_SERVICE`                | `ecs_service_name`                                |
    | `ECS_TASK_FAMILY`            | `solitar`                                         |
    | `FRONTEND_BUCKET`            | `frontend_bucket_name`                            |
    | `CLOUDFRONT_DISTRIBUTION_ID` | `cloudfront_distribution_id`                      |

## CI/CD (GitHub Actions, OIDC — no long-lived keys)

- **`.github/workflows/backend-deploy.yml`** — on `apps/backend/**`: builds the native ARM64 image on
  `ubuntu-latest` via docker buildx + QEMU (the repo is private, so the free `ubuntu-24.04-arm`
  runners are unavailable — the emulated build is slower but free), pushes to ECR (`:${sha}` +
  `:latest`), then renders the live (Terraform-owned) task definition with the new image and deploys
  to ECS with `wait-for-service-stability`. (If the repo is made public, switch `runs-on` to
  `ubuntu-24.04-arm` and drop the QEMU/buildx steps for a much faster native build.)
- **`.github/workflows/frontend-deploy.yml`** — on `apps/frontend/**`/`packages/**`: `pnpm generate`
  → `aws s3 sync` (hashed `_nuxt`/`_fonts` immutable, everything else `no-cache`, `--delete`) →
  CloudFront invalidation.

## Local development

- **Backend** — `docker compose up` starts the app + a single LocalStack (DynamoDB + SQS, tables and
  queues provisioned by `apps/backend/localstack-init/`). Tests use Testcontainers LocalStack:
  `mise exec -- ./gradlew test` (needs Docker).
- **Frontend** — `pnpm --filter @solitar/frontend dev` (or `vp dev`).

## Smoke test (post-deploy)

```bash
curl -I https://solitar.link/                     # 200, static (CloudFront -> S3)
# create a link via the UI or POST /create, then:
curl -I https://solitar.link/<code>               # 301 Location: <original>, no x-cache Hit
curl -s https://solitar.link/statistics           # totalClicks reflects the hit
curl -I https://solitar.link/definitelynotacode   # 404 (ALB), not the SPA
```
