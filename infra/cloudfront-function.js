// CloudFront viewer-request rewrite for the flat-file Nuxt SSG build (nitro autoSubfolderIndex:false,
// which emits /settings.html rather than /settings/index.html). Attached only to the S3 HTML route
// behaviors — short-link paths go to the ALB default behavior and never reach this function.
// NOTE: phase-05 owns the final logic; keep it in sync with the generated file layout.
function handler(event) {
    var request = event.request;
    var uri = request.uri;

    // Root -> index.html (also covered by default_root_object; explicit for safety).
    if (uri === '/') {
        request.uri = '/index.html';
        return request;
    }

    // Drop a single trailing slash; flat SSG has no per-route index.html.
    if (uri.length > 1 && uri.endsWith('/')) {
        uri = uri.slice(0, -1);
    }

    // Already a file (last segment has an extension) -> serve as-is.
    var lastSegment = uri.slice(uri.lastIndexOf('/') + 1);
    if (lastSegment.indexOf('.') !== -1) {
        request.uri = uri;
        return request;
    }

    // Extensionless route -> flat .html (e.g. /settings -> /settings.html).
    request.uri = uri + '.html';
    return request;
}
