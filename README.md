<p align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset=".github/assets/cover-dark.png">
        <img src=".github/assets/cover-light.png">
    </picture>
</p>

<p align="center">
    <img src="https://img.shields.io/github/issues-raw/solitar-dev/code?style=for-the-badge&logoColor=ffffff&label=ISSUES&labelColor=16A085&color=eff1f5" />
    <img src="https://img.shields.io/github/issues-pr-raw/solitar-dev/code?style=for-the-badge&logoColor=ffffff&label=PRs&labelColor=16A085&color=eff1f5" />
    <img src="https://img.shields.io/github/contributors/solitar-dev/code?style=for-the-badge&logoColor=ffffff&label=contributors&labelColor=16A085&color=eff1f5" />
    <img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fghloc.vercel.app%2Fapi%2Fsolitar-dev%2Fcode%2Fbadge&style=for-the-badge&logo=ffffff&label=lines&labelColor=16A085&color=eff1f5" />
    <img src="https://img.shields.io/github/commit-activity/m/solitar-dev/code?style=for-the-badge&logoColor=ffffff&label=commits&labelColor=16A085&color=eff1f5" />
    <img src="https://img.shields.io/github/last-commit/solitar-dev/code?style=for-the-badge&logoColor=ffffff&label=last%20commit&labelColor=16A085&color=eff1f5" />
</p>

---

## Solitar Monorepo

Solitar is an AWS-native URL shortener.

- **`apps/backend`** — Spring Boot 4 (Kotlin) compiled to a GraalVM native image, on ECS Fargate
  Spot behind an ALB. Persistence on DynamoDB, messaging on SQS, in-process Caffeine cache.
- **`apps/frontend`** — Nuxt 4 static site (`nuxt generate`) served from S3 + CloudFront.
- **`infra`** — Terraform for the whole AWS stack.

### Architecture & operations

- [docs/system-architecture.md](./docs/system-architecture.md) — topology and design trade-offs
- [docs/deployment-guide.md](./docs/deployment-guide.md) — build, CI/CD, GitHub variables, smoke tests
- [docs/aws-setup-checklist.md](./docs/aws-setup-checklist.md) — ordered first-time setup commands (native gate → infra → deploy)
- [docs/teardown-and-upgrade-runbook.md](./docs/teardown-and-upgrade-runbook.md) — clean teardown / upgrade before credit EOL
- [infra/README.md](./infra/README.md) — Terraform apply order and constraints

### Local development

```bash
# Backend + LocalStack (DynamoDB + SQS)
docker compose up

# Frontend
pnpm --filter @solitar/frontend dev
```

## License

This project is licensed under [MIT](./LICENSE) license.
