package com.scrimslegends.app.util

object ServerTimeProvider {
    /**
     * The delta between the server time and the local device time in milliseconds.
     * Calculated as: serverTimeMs - localTimeMs
     */
    var timeDeltaMs: Long = 0L

    /**
     * Returns the synchronized current time in milliseconds.
     * If the delta hasn't been fetched yet, it falls back to local device time.
     */
    fun getSynchronizedTime(): Long {
        return System.currentTimeMillis() + timeDeltaMs
    }
}
