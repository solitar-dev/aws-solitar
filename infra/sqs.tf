resource "aws_sqs_queue" "dlq" {
  name                      = "link-events-dlq"
  message_retention_seconds = 1209600 # 14 days

  tags = { Name = "${var.project}-dlq" }
}

resource "aws_sqs_queue" "link_created" {
  name                       = "link-created"
  visibility_timeout_seconds = 30

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 5
  })

  tags = { Name = "${var.project}-link-created" }
}

resource "aws_sqs_queue" "link_forwarded" {
  name                       = "link-forwarded"
  visibility_timeout_seconds = 30

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 5
  })

  tags = { Name = "${var.project}-link-forwarded" }
}

# Only the two work queues may use the DLQ as their redrive target.
resource "aws_sqs_queue_redrive_allow_policy" "dlq" {
  queue_url = aws_sqs_queue.dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.link_created.arn, aws_sqs_queue.link_forwarded.arn]
  })
}
