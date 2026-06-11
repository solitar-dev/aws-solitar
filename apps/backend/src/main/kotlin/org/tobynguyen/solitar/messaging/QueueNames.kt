package org.tobynguyen.solitar.messaging

/** Centralized SQS queue names (hyphenated — SQS standard-queue names disallow dots). */
object QueueNames {
    const val LINK_CREATED = "link-created"
    const val LINK_FORWARDED = "link-forwarded"
    const val DLQ = "link-events-dlq"
}
