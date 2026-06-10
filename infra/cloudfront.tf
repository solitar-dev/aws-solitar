locals {
  # S3 static assets (already have extensions) — no path rewrite needed.
  s3_asset_patterns = ["/_nuxt/*", "/fonts/*", "/__og-image__/*", "/favicon.ico", "/index.html"]
  # Extensionless frontend routes — the rewrite function appends `.html`.
  s3_route_patterns = ["/settings*", "/qr*", "/unlock*"]
}

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${var.project}-frontend-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_function" "rewrite" {
  name    = "${var.project}-rewrite"
  runtime = "cloudfront-js-2.0"
  comment = "Flat-file SSG path rewrite (autoSubfolderIndex:false)"
  publish = true
  code    = file("${path.module}/cloudfront-function.js")
}

data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

data "aws_cloudfront_cache_policy" "caching_disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_origin_request_policy" "all_viewer_except_host" {
  name = "Managed-AllViewerExceptHostHeader"
}

resource "aws_cloudfront_distribution" "main" {
  enabled             = true
  is_ipv6_enabled     = true
  aliases             = [var.domain_name]
  default_root_object = "index.html"
  price_class         = "PriceClass_100"
  comment             = "${var.project} edge"

  origin {
    origin_id                = "s3-frontend"
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  origin {
    origin_id   = "alb"
    domain_name = "alb.${var.domain_name}"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # Default: short-link redirects + API + SPA fallback -> ALB. Never cached, Host header stripped so
  # the ALB doesn't 400 on the viewer Host.
  default_cache_behavior {
    target_origin_id         = "alb"
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer_except_host.id
  }

  # Static assets -> S3, long cache, no rewrite.
  dynamic "ordered_cache_behavior" {
    for_each = local.s3_asset_patterns
    content {
      path_pattern           = ordered_cache_behavior.value
      target_origin_id       = "s3-frontend"
      viewer_protocol_policy = "redirect-to-https"
      allowed_methods        = ["GET", "HEAD"]
      cached_methods         = ["GET", "HEAD"]
      cache_policy_id        = data.aws_cloudfront_cache_policy.caching_optimized.id
    }
  }

  # Frontend HTML routes -> S3, with the `.html` rewrite function.
  dynamic "ordered_cache_behavior" {
    for_each = local.s3_route_patterns
    content {
      path_pattern           = ordered_cache_behavior.value
      target_origin_id       = "s3-frontend"
      viewer_protocol_policy = "redirect-to-https"
      allowed_methods        = ["GET", "HEAD"]
      cached_methods         = ["GET", "HEAD"]
      cache_policy_id        = data.aws_cloudfront_cache_policy.caching_optimized.id

      function_association {
        event_type   = "viewer-request"
        function_arn = aws_cloudfront_function.rewrite.arn
      }
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.cloudfront.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  tags = { Name = "${var.project}-cdn" }
}
