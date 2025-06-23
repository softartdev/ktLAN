package com.softartdev.ktlan.domain.repo

import com.softartdev.ktlan.domain.model.HostModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class ScanRepo {
    // TODO use a library or implement a scanning algorithm
    suspend fun scanRange(startIp: String, endIp: String, ports: List<Int>): List<HostModel> {
        val ipRangeList: List<String> = createRange(startIp, endIp)
        delay(duration = 5.seconds) // Simulate network delay
        return ipRangeList.map { ip ->
            val openPorts: List<Int> = ports.filter { port ->
                // Simulate port scanning logic
                // In a real implementation, you would check if the port is open
                // For now, we just simulate that all ports are open
                true
            }
            HostModel(ip, openPorts)
        }
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