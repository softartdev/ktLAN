package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.domain.model.HostModel
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ScanRepo {
    private val client: HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) = Napier.d(tag = "Ktor", message = message)
            }
        }
        followRedirects = true
    }

    // Scans a range of IP addresses for open ports parallelly using coroutines Jobs.
    suspend fun scanRangeParallel(
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
                    Napier.d("Scanning port $portInt on $ip")
                    try {
                        val response = client.get {
                            timeout {
                                requestTimeoutMillis = 5000 // Set a timeout for the request
                                connectTimeoutMillis = 5000 // Set a timeout for the connection
                                socketTimeoutMillis = 5000 // Set a timeout for the socket
                            }
                            url { host = ip; port = portInt }
                        }
                        Napier.d("$ip:$portInt ✅${response.status.value}:${response.status.description}")
                        resultMap[ip]?.add(portInt)
                    } catch (e: ConnectTimeoutException) {
//                        Napier.e(message = e.message ?: "Connection timeout")
                    } catch (e: Throwable) {
                        if (e.message?.contains("timeout") == true) {
//                            Napier.e(message = e.message ?: "$ip:$portInt ❌timeout")
                        } else if (e.message?.contains("Connection refused") == true) {
//                            Napier.d("$ip:$portInt ❌", e)
                            resultMap[ip]?.add(portInt)
                        } else if (e.message?.contains("Network is unreachable") == true) {
//                            Napier.d("$ip:$portInt ❌", e)
                        } else {
                            Napier.e("$ip:$portInt ❌", e)
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
        Napier.d("Parallel scan completed for range $startIp to $endIp with ${hosts.size} hosts found.")
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