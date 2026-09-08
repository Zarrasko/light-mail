package com.thelightphone.mail

/** Pure logic for how many messages to fetch, split out so it's unit-testable without a server. */
object MailInboxFetchPlanner {

    /** The account's own limit if it has one, else [globalDefault]'s. 0 means unlimited. */
    fun effectiveLimit(accountOverride: Int?, globalDefault: MailMessageLimit): Int =
        accountOverride ?: globalDefault.count

    /**
     * The IMAP sequence-number range to fetch for a mailbox with [messageCount] messages, given
     * an effective [limit] (0 = unlimited, fetch everything). Returns null if there's nothing to
     * fetch (an empty mailbox).
     */
    fun sequenceRangeFor(messageCount: Int, limit: Int): String? {
        if (messageCount <= 0) return null
        if (limit <= 0) return "1:$messageCount"
        val start = maxOf(1, messageCount - limit + 1)
        return "$start:$messageCount"
    }
}
