package com.softartdev.ktlan.domain.model

data class HostModel(
    val ip: String,
    val openPorts: List<Int>
)