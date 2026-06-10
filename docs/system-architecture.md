# System Architecture

Solitar is an AWS-native URL shortener. Stats accuracy is the governing constraint: **every redirect
reaches the backend** (CloudFront never caches redirects) so click counts stay exact.

## Topology

```
Route 53 (solitar.link)
   └── CloudFront
        ├── /_nuxt/* /_fonts/* /_og/* /_i18n/* /*.png /settings* /qr* /index.html
        │      → S3 (OAC, private)         [static Nuxt, cached]
        └── *  (CachingDisabled, AllViewerExceptHostHeader)
               → ALB (alb.solitar.link, 443) → ECS Fargate Spot (1× ARM64, public subnet)
                    ├── GET /{code} → DynamoDB urls → 301 + publish link-forwarded
                    ├── POST /create → code-gen + conditional put → publish link-created
                    └── GET /statistics → DynamoDB statistics
   SQS link-created / link-forwarded → @SqsListener → DynamoDB statistics (atomic UpdateItem ADD)
   DLQ: link-events-dlq (maxReceiveCount 5)
```

## Components

| Concern     | Choice                                              | Notes                                                                                              |
| ----------- | --------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Edge        | CloudFront + Route 53                               | Apex → CloudFront; `alb.` subdomain → ALB (regional cert CN matches origin).                       |
| Static site | S3 (OAC) + Nuxt SSG                                 | `nuxt generate`, flat `.html` (`autoSubfolderIndex:false`); a CloudFront Function appends `.html`. |
| Compute     | ECS Fargate **Spot**, 1× ARM64 (0.25 vCPU / 0.5 GB) | Public subnet + public IP, **no NAT**; ALB locked to the CloudFront origin-facing prefix list.     |
| Runtime     | Spring Boot 4 + GraalVM native                      | Fast cold start; reachability hints in `NativeRuntimeHints`.                                       |
| Persistence | DynamoDB on-demand (`urls`, `statistics`)           | Enhanced Client `@DynamoDbImmutable`; counters via atomic `ADD`.                                   |
| Messaging   | SQS + `@SqsListener` (Spring Cloud AWS)             | At-least-once; `ADD` is commutative so redelivery is safe.                                         |
| Cache       | Caffeine (in-process)                               | Caches the URL lookup only; the forwarded event still fires every hit.                             |
| Rate limit  | Bucket4j in-memory                                  | Single-task scoped (resets on restart / second task).                                              |
| Code-gen    | Random Base62(7) + conditional write + retry        | Replaced the old KGS service; reserved-word guard.                                                 |

## Request flows

- **Redirect** — CloudFront default `*` (uncached, Host stripped) → ALB → Fargate `GET /{code}` →
  DynamoDB `getItem` (Caffeine in front) → publish `link-forwarded` → `301`. The aggregator increments
  `totalClicks` asynchronously.
- **Create** — `POST /create` → generate code → DynamoDB conditional put (`attribute_not_exists`) with
  bounded retry → publish `link-created` → return code.
- **Static** — CloudFront enumerated behavior → CloudFront Function (`/settings` → `/settings.html`) →
  S3 over OAC.

## Deliberate trade-offs

- **No NAT gateway** — biggest free-plan cost trap avoided; tasks reach ECR/SQS over a public IP and
  DynamoDB over a free gateway endpoint.
- **Single Spot task** — ~2-min interruption window accepted for cost; deployment circuit breaker +
  rollback, `min_healthy_percent=0`.
- **Redirects never cached** — stats exactness beats edge-cache latency (the project's whole point).
- **IAM, not secrets** — task role for AWS access; no DB passwords, no static keys (OIDC in CI).

See [deployment-guide.md](./deployment-guide.md) and [teardown-and-upgrade-runbook.md](./teardown-and-upgrade-runbook.md).
