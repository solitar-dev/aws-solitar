# ElastiCache Serverless (Valkey) — the redirect cache. Private to the VPC, reachable only from the
# Fargate task SG on 6379, TLS-only (a serverless requirement). Cost is hard-capped via
# cache_usage_limits so a runaway loop can't blow the budget. Reuses the predecessor's VPC + public
# subnets + task SG; the only new infra is one cache, one SG, and its rules.

resource "aws_security_group" "valkey" {
  name        = "${var.project}-valkey"
  description = "ElastiCache Valkey: 6379 from the Fargate task SG only"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.project}-valkey" }
}

# Inbound 6379 from the task SG only — mirrors the task_from_alb pattern in security.tf. No CIDR, no
# public ingress: the cache is reachable solely by the Fargate task.
resource "aws_vpc_security_group_ingress_rule" "valkey_from_task" {
  security_group_id            = aws_security_group.valkey.id
  description                  = "Valkey 6379 from the Fargate task"
  ip_protocol                  = "tcp"
  from_port                    = 6379
  to_port                      = 6379
  referenced_security_group_id = aws_security_group.task.id
}

# Egress all for parity with the other SGs (the cache initiates nothing, but keeps the SG well-formed).
resource "aws_vpc_security_group_egress_rule" "valkey_all" {
  security_group_id = aws_security_group.valkey.id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_elasticache_serverless_cache" "valkey" {
  name   = "${var.project}-valkey"
  engine = "valkey"

  # Hard cost ceiling. 1 GB storage + 1000 ECPU/s are the AWS-minimum caps — far more than a
  # single-task redirect cache (tiny code->URL strings, 24h TTL) ever needs, and raisable later
  # without a recreate. The bill tracks actual use (~100 MB floor + pennies of ECPU, ~$6-7/mo), not
  # the cap; the cap only stops a hot loop from running the ECPU bill away.
  cache_usage_limits {
    data_storage {
      maximum = 1
      unit    = "GB"
    }
    ecpu_per_second {
      maximum = 1000
    }
  }

  security_group_ids = [aws_security_group.valkey.id]
  subnet_ids         = aws_subnet.public[*].id

  tags = { Name = "${var.project}-valkey" }
}
