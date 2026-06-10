# Teardown & Upgrade Runbook

The AWS account is funded by free-plan credits that exhaust in roughly six months, after which the
account auto-closes. This runbook tears the stack down cleanly, or upgrades before the deadline.

## Before the credit deadline — decide

- **Keep running** → upgrade the account to a paid plan **~1 month before** credit exhaustion (credits
  generally carry for 12 months; the account closes when they run out and you have not upgraded).
  Watch the `solitar-monthly` budget alarm (≈$25 forecasted) — confirm its email subscription so it
  actually fires.
- **Shut down** → run the teardown below.

## Teardown (order matters)

> `terraform destroy` strands on a live DNS delegation and on non-empty buckets/repos if you skip the
> ordering. `force_destroy` (S3) and `force_delete` (ECR) are already set; certs use
> `create_before_destroy`.

1. **Move DNS off Route 53 first.** At the `.link` registrar, point the nameservers back to the
   previous provider (or park them). This prevents an in-flight ACM renewal/validation from blocking
   destroy and stops serving traffic from about-to-be-deleted infra.
2. **Destroy.**
    ```bash
    cd infra
    terraform destroy
    ```
    `force_destroy`/`force_delete` empty the S3 frontend bucket and the ECR repo automatically.
3. **Verify the console is empty** — no leftover: S3 buckets, ECR images, ACM certs, Route 53 zone,
   CloudFront distribution, ALB, ECS cluster/service, DynamoDB tables, SQS queues, VPC. CloudFront
   distributions take ~15 min to fully disable/delete; re-check if `destroy` reports it still in
   progress.
4. **State** is local (`infra/terraform.tfstate`, gitignored). Keep a copy if you want a record;
   otherwise it goes with the directory.

## Gotchas

- **CloudFront + ACM** deletion is slow and global; a `destroy` may need a re-run once the
  distribution finishes disabling.
- **Budget alarm** lives in AWS Budgets (not Terraform-region-bound) — `destroy` removes it; the
  account-close safety then rests on the calendar reminder, not the alarm.
- **OIDC provider** is account-global; `destroy` removes the Solitar role + provider. If other stacks
  reuse the GitHub OIDC provider, detach instead of destroy (not the case here).

## Cost sanity (while running)

ALB ~$17/mo is the floor; Fargate Spot ~$2–3, Route 53 ~$0.50, DynamoDB/SQS within always-free,
S3/CloudFront pennies. ≈ **$21–24/mo**, inside the budget alarm. The biggest avoidable cost — a NAT
gateway (~$32/mo) — is deliberately absent (public-subnet tasks + DynamoDB gateway endpoint).

## Future cost cut (optional)

Swapping the ALB for API Gateway HTTP API would drop the ~$17/mo floor to per-request pricing — out
of scope for this migration, noted for when traffic is low enough to favor it.
