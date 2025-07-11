package com.softartdev.ktlan.data.webrtc

interface IConsole {

    fun printf(text: String, vararg args: Any)

    fun d(text: String, vararg args: Any) {
        printf("<font color=\"#673AB7\">$text</font>")
    }

    fun i(text: String, vararg args: Any) {
        greenf(text, args)
    }

    fun e(text: String, vararg args: Any) {
        redf(text, args)
    }

    fun greenf(text: String, vararg args: Any) {
        printf("<font color=\"#009900\">$text</font>")
    }

    fun bluef(text: String, vararg args: Any) {
        printf("<font color=\"#000099\">$text</font>")
    }

    fun redf(text: String, vararg args: Any) {
        printf("<font color=\"#990000\">$text</font>")
    }
}