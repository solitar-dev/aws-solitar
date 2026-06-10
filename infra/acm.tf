# CloudFront viewer cert MUST be in us-east-1 (alias provider declared in terraform.tf). Covers the
# apex + wildcard so alb.<domain> is also valid if ever served directly.
resource "aws_acm_certificate" "cloudfront" {
  provider                  = aws.us_east_1
  domain_name               = var.domain_name
  subject_alternative_names = ["*.${var.domain_name}"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = { Name = "${var.project}-cloudfront-cert" }
}

# Regional cert for the ALB listener. CloudFront connects to the origin as alb.<domain> so this
# cert's CN matches the origin hostname (avoids an origin SSL handshake failure).
resource "aws_acm_certificate" "alb" {
  domain_name       = "alb.${var.domain_name}"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = { Name = "${var.project}-alb-cert" }
}

resource "aws_acm_certificate_validation" "cloudfront" {
  provider                = aws.us_east_1
  certificate_arn         = aws_acm_certificate.cloudfront.arn
  validation_record_fqdns = [for r in aws_route53_record.cloudfront_cert_validation : r.fqdn]
}

resource "aws_acm_certificate_validation" "alb" {
  certificate_arn         = aws_acm_certificate.alb.arn
  validation_record_fqdns = [for r in aws_route53_record.alb_cert_validation : r.fqdn]
}
