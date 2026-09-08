package com.thelightphone.mail

/** How many messages an inbox fetches/shows. [count] of 0 means unlimited. */
enum class MailMessageLimit(val count: Int, val label: String) {
    TEN(10, "10"),
    TWENTY(20, "20"),
    FIFTY(50, "50"),
    UNLIMITED(0, "Unlimited"),
    ;

    companion object {
        val DEFAULT = TWENTY

        fun fromCount(count: Int): MailMessageLimit = entries.firstOrNull { it.count == count } ?: DEFAULT
    }
}
