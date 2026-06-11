# AWS First-Time Setup Checklist

The ordered path from the merged migration to a live stack. Runs on your machine — needs AWS admin
credentials, `terraform` >= 1.10, `docker` with buildx, the `aws`/`gh`/`dig` CLIs, and access to the
`.link` domain registrar. See [deployment-guide.md](./deployment-guide.md) for the architecture-level
overview and [infra/README.md](../infra/README.md) for the Terraform design constraints.

## Phase A — Prove the native image boots (do this first)

Don't provision paid infra until the GraalVM native image actually boots. This is the migration's hard
gate.

```bash
# from repo root, on a Docker host
docker compose up --build        # builds the native image (~5-10 min) + LocalStack

# in another shell, once the 'solitar' container is healthy:
curl -sX POST localhost:8080/create -H 'content-type: application/json' \
  -d '{"url":"https://example.com/x"}'          # -> {"shortCode":"...", ...}
curl -i localhost:8080/<shortCode>               # -> 301 Location: https://example.com/x
docker compose logs solitar | grep -i "ClassNotFound\|reflect"   # must be empty
```

If a `ClassNotFoundException` appears (most likely a Caffeine cache class), add it to
`apps/backend/src/main/kotlin/org/tobynguyen/solitar/config/NativeRuntimeHints.kt` and rebuild. The
Caffeine class list there is a best-effort starting set that the real native build must confirm. The
gate passes when the container boots and 301s cleanly with no reflection errors in the logs.

## Phase B — Provision infra (mind the DNS ordering)

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars
#   edit: github_repo = "solitar-dev/aws-solitar"
terraform init

# 1. Create ONLY the hosted zone first:
terraform apply -target=aws_route53_zone.main
terraform output route53_nameservers            # note the 4 awsdns nameservers

# 2. At your .link registrar, set the domain's nameservers to those 4 values.
# 3. Wait for delegation to go live (minutes to hours):
dig +short NS solitar.link @8.8.8.8              # must return the awsdns nameservers

# 4. Now apply everything — ACM DNS validation resolves via the delegated zone:
terraform apply
```

The one ordering trap: if you run step 4 before the nameservers are live at the registrar,
`aws_acm_certificate_validation` hangs waiting for DNS that can't resolve yet. After the apply,
**confirm the budget-alarm email** that AWS Budgets sends (one-time subscription click).

## Phase C — First image + GitHub variables

The ECS task definition references `:latest`, so push one image before the service can start a task.

```bash
ECR_URL=$(terraform -chdir=infra output -raw ecr_repository_url)
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin "${ECR_URL%/*}"
docker buildx build --platform linux/arm64 -f apps/backend/Dockerfile -t "$ECR_URL:latest" --push .
aws ecs update-service --cluster solitar --service solitar --force-new-deployment
```

Set the GitHub OIDC variables (run from repo root; OIDC means no long-lived secret keys):

```bash
gh variable set AWS_OIDC_ROLE_ARN          --body "$(terraform -chdir=infra output -raw github_oidc_role_arn)"
gh variable set ECR_REPOSITORY             --body "solitar"
gh variable set ECS_CLUSTER                --body "$(terraform -chdir=infra output -raw ecs_cluster_name)"
gh variable set ECS_SERVICE                --body "$(terraform -chdir=infra output -raw ecs_service_name)"
gh variable set ECS_TASK_FAMILY            --body "solitar"
gh variable set FRONTEND_BUCKET            --body "$(terraform -chdir=infra output -raw frontend_bucket_name)"
gh variable set CLOUDFRONT_DISTRIBUTION_ID --body "$(terraform -chdir=infra output -raw cloudfront_distribution_id)"
```

## Phase D — Merge, auto-deploy, smoke

```bash
gh pr merge 2 --squash          # triggers backend-deploy + frontend-deploy (vars now set)
gh run watch                    # watch them go green

# smoke (after the deploys finish):
curl -I https://solitar.link/                  # 200, served from S3
# create a link via the UI or POST /create, then:
curl -I https://solitar.link/<code>            # 301, no 'x-cache: Hit'
curl -s https://solitar.link/statistics        # totalClicks reflects the hit
curl -I https://solitar.link/notarealcode      # 404 (ALB), not the SPA
```

## Notes

- **Private repo:** the backend image builds under QEMU emulation (slow, ~20-40 min). When you make
  the repo public, switch `runs-on` to `ubuntu-24.04-arm` in `.github/workflows/backend-deploy.yml`
  and drop the QEMU/buildx setup steps for a much faster native build.
- **OG images:** the frontend's `nuxt-og-image` Takumi renderer is set non-fatal
  (`nitro.prerender.failOnError: false`), so social previews may be missing until that renderer is
  fixed — it does not block the build or deploy.
- **Teardown / credit EOL:** see [teardown-and-upgrade-runbook.md](./teardown-and-upgrade-runbook.md)
  — move the registrar nameservers off Route 53 *before* `terraform destroy`.
