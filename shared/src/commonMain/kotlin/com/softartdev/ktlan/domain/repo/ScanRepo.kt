package com.softartdev.ktlan.domain.repo

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.softartdev.ktlan.domain.model.HostModel
import com.softartdev.ktlan.domain.util.KermitKtorLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Repository for network scanning operations.
 * Stateless data access layer following Clean Architecture principles.
 */
open class ScanRepo {
    private val logger = Logger.withTag("ScanRepo")
    private val client: HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.BODY
            logger = KermitKtorLogger(Severity.Debug, Logger.withTag("Ktor"))
        }
        followRedirects = true
    }

    // Scans a range of IP addresses for open ports parallelly using coroutines Jobs.
    open suspend fun scanRangeParallel(
        coroutineContext: CoroutineContext,
        startIp: String,
        endIp: String,
        ports: List<Int>
    ): List<HostModel> {
        val ipRangeList: List<String> = createRange(startIp, endIp)
        val coroutineScope = CoroutineScope(coroutineContext)
        val jobs: MutableList<Job> = mutableListOf()
        val resultMap: MutableMap<String, MutableList<Int>> = mutableMapOf()
        ipRangeList.map { ip: String ->
            resultMap[ip] = mutableListOf()
            for (portInt: Int in ports) {
                val job = coroutineScope.launch {
                    logger.d { "Scanning port $portInt on $ip" }
                    try {
                        val response = client.get {
                            timeout {
                                requestTimeoutMillis = 5000 // Set a timeout for the request
                                connectTimeoutMillis = 5000 // Set a timeout for the connection
                                socketTimeoutMillis = 5000 // Set a timeout for the socket
                            }
                            url { host = ip; port = portInt }
                        }
                        logger.d { "$ip:$portInt ✅${response.status.value}:${response.status.description}" }
                        resultMap[ip]?.add(portInt)
                    } catch (e: ConnectTimeoutException) {
//                        logger.e { e.message ?: "Connection timeout" }
                    } catch (e: Throwable) {
                        if (e.message?.contains("timeout") == true) {
//                            logger.e { e.message ?: "$ip:$portInt ❌timeout" }
                        } else if (e.message?.contains("Connection refused") == true) {
//                            logger.d(e) { "$ip:$portInt ❌" }
                            resultMap[ip]?.add(portInt)
                        } else if (e.message?.contains("Network is unreachable") == true) {
//                            logger.d(e) { "$ip:$portInt ❌" }
                        } else {
                            logger.e(e) { "$ip:$portInt ❌" }
                        }
                    }
                }
                jobs.add(job)
            }
        }
        jobs.joinAll()
        val hosts: List<HostModel> = resultMap
            .filter { (_, openPorts) -> openPorts.isNotEmpty() }
            .map { (ip, openPorts) -> HostModel(ip, openPorts.sorted()) }
            .sortedBy(HostModel::ip)
        logger.d { "Parallel scan completed for range $startIp to $endIp with ${hosts.size} hosts found." }
        return hosts
    }

    private fun createRange(startIp: String, endIp: String): List<String> {
        val result: MutableList<String> = mutableListOf()
        val startParts: List<Int> = startIp.split(".").map(String::toInt)
        val endParts: List<Int> = endIp.split(".").map(String::toInt)
        for (i in startParts[0]..endParts[0]) {
            for (j in startParts[1]..endParts[1]) {
                for (k in startParts[2]..endParts[2]) {
                    for (l in startParts[3]..endParts[3]) {
                        result.add("$i.$j.$k.$l")
                    }
                }
            }
        }
        return result
    }
}