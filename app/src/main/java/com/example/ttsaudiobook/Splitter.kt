package com.example.ttsaudiobook

/**
 * Utility responsible for splitting a very long text into smaller chunks that are easier
 * for the Text‑To‑Speech engine to handle.  A chunk is limited to [MAX_LEN] characters and
 * will end on the last hyphen ('-') encountered before the limit if one exists.  If no
 * hyphen is found within a reasonable distance (we require it to be more than
 * [MIN_SPLIT_THRESHOLD] characters away from the start of the current chunk to avoid
 * creating very small leading fragments) then the text is simply split at the maximum
 * length.
 */
object Splitter {
    /**
     * Maximum number of characters allowed per chunk.  Adjust this constant if you need
     * smaller or larger chunks.  The value 20_000 is chosen to balance memory usage and
     * synthesis stability on mid‑range devices.
     */
    const val MAX_LEN: Int = 20_000

    /**
     * Minimum number of characters before we consider a hyphen as a valid split point.
     * Without this threshold the algorithm might choose a hyphen very early in the window
     * and produce a trivial first chunk; that would defeat the purpose of having large
     * chunks.  Here we require at least 1 000 characters before a hyphen can be used as
     * a split point.
     */
    private const val MIN_SPLIT_THRESHOLD: Int = 1_000

    /**
     * Split the given [text] into a list of chunks according to the smart hyphen rule.
     *
     * The algorithm iteratively examines windows of at most [MAX_LEN] characters.  Within
     * each window it searches backwards for the last hyphen ('-').  If a hyphen exists
     * and its position is greater than [MIN_SPLIT_THRESHOLD], the text is cut at that
     * hyphen.  Otherwise the cut happens exactly at [MAX_LEN].  This ensures that
     * chunks are as large as possible while still respecting user‑inserted hyphens.
     *
     * @param text the full input string to split
     * @return a list of chunk strings (never empty)
     */
    fun split(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        val length = text.length
        // Guard against empty input
        if (length == 0) return listOf("")
        while (start < length) {
            // Determine the furthest index we can consider in this iteration.  We do not
            // exceed the end of the input.
            val endLimit = (start + MAX_LEN).coerceAtMost(length)
            // Create the current window substring for searching.  Note that substring
            // operations copy characters in Java/Kotlin; however this overhead is small
            // compared to the TTS synthesis that follows.
            val window = text.substring(start, endLimit)
            // Find the last occurrence of '-' within the window.  If there is no
            // hyphen the result is -1.
            val lastHyphen = window.lastIndexOf('-')
            // Decide where to split.  Use the hyphen only if it appears past the
            // threshold; otherwise fall back to splitting at the end limit.
            val splitPoint = if (lastHyphen != -1 && lastHyphen > MIN_SPLIT_THRESHOLD) {
                start + lastHyphen
            } else {
                endLimit
            }
            // Extract the chunk and trim whitespace/newlines from its ends.  Trimming
            // avoids leading/trailing spaces that can cause unnatural speech breaks.
            val chunk = text.substring(start, splitPoint).trim()
            chunks.add(chunk)
            // Advance start index past the split point and the hyphen itself.  This
            // ensures the hyphen is not included at the beginning of the next chunk.
            start = splitPoint + 1
        }
        return chunks
    }
}