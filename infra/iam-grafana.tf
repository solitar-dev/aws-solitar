# Grafana Cloud reads CloudWatch metrics through this dedicated, read-only IAM user. This is the one
# accepted exception to the project's "no static keys" rule (R-B): the user is read-only,
# CloudWatch-scoped, and single-purpose, and its access key is minted MANUALLY (console or
# `aws iam create-access-key`) and stored ONLY in Grafana — never created by Terraform, never in
# state, never in the repo. See docs/deployment-guide.md "Grafana Cloud setup".

resource "aws_iam_user" "grafana_cloud_readonly" {
  name = "grafana-cloud-readonly"

  tags = { Name = "grafana-cloud-readonly" }
}

# AWS-managed read-only CloudWatch access (metrics list + GetMetricData + alarms, plus logs read).
# Enough for the Grafana CloudWatch datasource to populate every panel. If Grafana template
# variables that resolve resources by tag are added later, also attach a policy granting
# `tag:GetResources` — not needed for the committed dashboard.
resource "aws_iam_user_policy_attachment" "grafana_cloudwatch_readonly" {
  user       = aws_iam_user.grafana_cloud_readonly.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchReadOnlyAccess"
}

# NOTE: there is deliberately NO `aws_iam_access_key` resource here. Creating the key in Terraform
# would persist the secret in state. Mint it by hand (see the deployment guide) and paste it into
# Grafana only.

output "grafana_iam_user_name" {
  description = "Read-only IAM user for the Grafana Cloud CloudWatch datasource. Mint its access key MANUALLY (aws iam create-access-key) and store it only in Grafana — never in Terraform state or the repo."
  value       = aws_iam_user.grafana_cloud_readonly.name
}
