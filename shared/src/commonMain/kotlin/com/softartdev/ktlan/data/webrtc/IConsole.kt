package com.softartdev.ktlan.data.webrtc

interface IConsole {
    /**
     * Print formatted text with optional arguments
     */
    fun printf(text: String, vararg args: Any)

    /**
     * Print debug information
     */
    fun debug(text: String, vararg args: Any) {
        printf(text, *args)
    }

    /**
     * Print info information
     */
    fun info(text: String, vararg args: Any) {
        printf(text, *args)
    }

    /**
     * Print error information
     */
    fun error(text: String, vararg args: Any) {
        printf(text, *args)
    }

    /**
     * Print success information
     */
    fun success(text: String, vararg args: Any) {
        printf(text, *args)
    }

    /**
     * Print warning information
     */
    fun warning(text: String, vararg args: Any) {
        printf(text, *args)
    }
}