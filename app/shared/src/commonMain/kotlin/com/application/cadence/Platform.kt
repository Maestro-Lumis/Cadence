package com.application.cadence

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform