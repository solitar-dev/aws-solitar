# On-demand tables stay inside the always-free 25GB / 200M-request tier. DynamoDB is schemaless
# beyond the key, so only the partition key `id` is declared; the app writes the rest.
resource "aws_dynamodb_table" "urls" {
  name         = "urls"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = { Name = "${var.project}-urls" }
}

resource "aws_dynamodb_table" "statistics" {
  name         = "statistics"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = { Name = "${var.project}-statistics" }
}
