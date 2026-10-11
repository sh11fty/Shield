package com.guardian.app

/** Pure-Kotlin text classifier (no Android deps so it can be unit-tested on a plain JVM). */
data class Hit(val kind: String, val detail: String)

object Matcher {
    private val LEET = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '@' to 'a', '$' to 's', '!' to 'i'
    )
    private val HOST = Regex("(?<![a-z0-9-])(?:[a-z0-9][a-z0-9-]*\\.)+[a-z]{2,24}(?![a-z0-9-])")

    private class Compiled(val sig: Int, val domains: Set<String>, val strong: Regex?, val mild: Regex?)

    @Volatile private var cache: Compiled? = null

    /** Large downloaded domain set (already normalized). Swapped atomically by RemoteList. */
    @Volatile var remote: Set<String> = emptySet()

    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s.lowercase()) sb.append(LEET[ch] ?: ch)
        return sb.toString()
    }

    private fun alt(words: Collection<String>): String =
        words.map { normalize(it.trim()) }.filter { it.isNotEmpty() }.distinct()
            .joinToString("|") { w ->
                // a trailing "+" means "one or more of the last letter"; spaces match any whitespace run
                w.split(' ').filter { it.isNotEmpty() }.joinToString("\\s+") { part ->
                    if (part.endsWith("+")) Regex.escape(part.dropLast(1)) + "+" else Regex.escape(part)
                }
            }

    private fun compile(domains: Collection<String>, strong: Collection<String>, mild: Collection<String>): Compiled {
        val sig = listOf(domains.size, strong.size, mild.size, domains.hashCode(), strong.hashCode(), mild.hashCode()).hashCode()
        cache?.let { if (it.sig == sig) return it }
        val d = domains.map { normalize(it.trim()).removePrefix("www.") }.filter { it.isNotEmpty() }.toHashSet()
        val s = alt(strong).takeIf { it.isNotEmpty() }?.let { Regex("(?<![a-z])(?:$it)") }
        val m = alt(mild).takeIf { it.isNotEmpty() }?.let { Regex("(?<![a-z0-9])(?:$it)(?![a-z])") }
        return Compiled(sig, d, s, m).also { cache = it }
    }

    fun evaluate(
        raw: String,
        domains: Collection<String>,
        strong: Collection<String>,
        mild: Collection<String>,
        threshold: Int = 3
    ): Hit? {
        if (raw.length < 3) return null
        val c = compile(domains, strong, mild)
        val text = normalize(raw.take(12000))

        for (m in HOST.findAll(text)) {
            val labels = m.value.removePrefix("www.").split('.')
            for (i in 0 until labels.size - 1) {
                val cand = labels.drop(i).joinToString(".")
                if (cand in c.domains || cand in remote) return Hit("domain", cand)
            }
        }
        var score = 0
        val seen = HashSet<String>()
        c.strong?.findAll(text)?.forEach { if (seen.add(it.value.replace(Regex("\\s+"), " "))) score += 3 }
        if (score >= threshold) return Hit("keyword", "strong")
        c.mild?.findAll(text)?.forEach { if (seen.add(it.value.replace(Regex("\\s+"), " "))) score += 1 }
        if (score >= threshold) return Hit("keyword", "combined")
        return null
    }
}
