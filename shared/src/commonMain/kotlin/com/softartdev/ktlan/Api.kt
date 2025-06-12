package com.softartdev.ktlan

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url

class ApplicationApi {
    private val client: HttpClient = HttpClient(CIO) {
        followRedirects = false
    }
    private val address = Url("http://checkip.amazonaws.com/")

    suspend fun loadAbout(): String = client.get { url(address) }.bodyAsText()
}
