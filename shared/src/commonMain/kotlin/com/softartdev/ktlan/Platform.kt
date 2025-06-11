package com.softartdev.ktlan

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform