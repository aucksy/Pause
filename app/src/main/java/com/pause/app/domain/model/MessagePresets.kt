package com.pause.app.domain.model

/**
 * The witty default messages shown on the interruption overlay, plus the limit for a user's own
 * custom message. Each preset is in the "emoji + short line" format and is intentionally gentle —
 * a nudge, never a scolding.
 */
object MessagePresets {

    /** Max length of a custom overlay message (counted in code points so emoji aren't split). */
    const val MAX_MESSAGE_LENGTH = 80

    val messages: List<String> = listOf(
        "Doom scrolling Again 😏",
        "🥱 Still scrolling?",
        "👀 Caught in the loop",
        "🧠 Your brain wants a break",
        "🌀 Down the rabbit hole again?",
        "⏳ Poof. There goes your time.",
        "🛑 Worth it? Be honest.",
        "🙃 Oh, we're still here?",
        "🐟 You took the bait.",
        "☕ Go touch some grass.",
        "💤 Doomscroll detected.",
    )

    /** The out-of-the-box message (also the value "Reset to default" restores). */
    val default: String = messages.first()

    /** Trim a candidate message to the limit without splitting a surrogate pair (emoji). */
    fun clamp(message: String): String {
        val trimmed = message.trim()
        if (trimmed.codePointCount(0, trimmed.length) <= MAX_MESSAGE_LENGTH) return trimmed
        val end = trimmed.offsetByCodePoints(0, MAX_MESSAGE_LENGTH)
        return trimmed.substring(0, end)
    }
}
