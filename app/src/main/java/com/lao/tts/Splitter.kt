package com.lao.tts

/**
 * Splits long strings into chunks that do not exceed [MAX_LEN] characters.
 * When the text length exceeds the limit, the algorithm searches backwards
 * from the limit for the last hyphen ('-') and splits there.  If no hyphen
 * exists in the window, the split occurs exactly at [MAX_LEN].
 */
object Splitter {

    /** Maximum number of characters allowed in a single chunk. */
    private const val MAX_LEN = 20_000

    /**
     * Performs the splitting logic on [input] and returns a list of chunks.
     */
    fun split(input: String): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < input.length) {
            val endCandidate = (start + MAX_LEN).coerceAtMost(input.length)
            var end = endCandidate
            if (end < input.length) {
                // Search backwards for a hyphen within the last 500 characters.
                val windowStart = (end - 500).coerceAtLeast(start)
                val index = input.lastIndexOf('-', startIndex = end - 1)
                if (index >= windowStart) {
                    end = index + 1 // Include hyphen in the previous chunk
                }
            }
            chunks.add(input.substring(start, end))
            start = end
        }
        return chunks
    }
}