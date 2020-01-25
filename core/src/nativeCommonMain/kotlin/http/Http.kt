@file:Suppress("ObjectPropertyName")

package com.epam.drill.hook.http

import com.epam.drill.hook.*
import com.epam.drill.hook.gen.*
import kotlinx.cinterop.*
import platform.posix.ssize_t
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

const val HTTP_DETECTOR_BYTES_COUNT = 8

const val HTTP_RESPONSE_MARKER = "HTTP"

const val FIRST_INDEX = 0

@SharedImmutable
val HTTP_VERBS =
    setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT") + HTTP_RESPONSE_MARKER

@SharedImmutable
val CR_LF = "\r\n"

@SharedImmutable
val CR_LF_BYTES = CR_LF.encodeToByteArray()

@SharedImmutable
val HEADERS_DELIMITER = CR_LF_BYTES + CR_LF_BYTES

@SharedImmutable
private val accessThread = Worker.start(true)

@ThreadLocal
private var _httpHook: CPointer<funchook_t>? = null

@ThreadLocal
private var reader = mutableMapOf<DRILL_SOCKET, ByteArray?>()

@ThreadLocal
private val _httpReadCallbacks = mutableSetOf<(ByteArray?) -> Unit>()

@ThreadLocal
private var _httpWriteCallback: (() -> Map<String, String>) = { emptyMap() }

var httpHook
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _httpHook }.result
    set(value) = accessThread.execute(TransferMode.UNSAFE, { value }) { _httpHook = it }.result

val httpReadCallbacks
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _httpReadCallbacks }.result

val httpWriteCallback: (() -> Map<String, String>)
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _httpWriteCallback }.result

fun addHttpReadCallbacks(httpRequest: (ByteArray?) -> Unit) =
    accessThread.execute(TransferMode.UNSAFE, { httpRequest }) { _httpReadCallbacks += it }

fun addHttpWriteCallback(writeCallback: () -> Map<String, String>) =
    accessThread.execute(TransferMode.UNSAFE, { writeCallback }) { _httpWriteCallback = it }

fun configureHttpHooksBuild(block: () -> Unit) = if (httpHook != null) {
    funchook_install(httpHook, 0).check("funchook_install")
} else {
    httpHook = funchook_create() ?: run {
        println("Failed to create hook")
        return
    }
    funchook_prepare(httpHook, read_func_point, staticCFunction(::drillRead)).check("prepare read_func_point")
    funchook_prepare(httpHook, write_func_point, staticCFunction(::drillWrite)).check("prepare write_func_point")
    funchook_prepare(httpHook, send_func_point, staticCFunction(::drillSend)).check("prepare send_func_point")
    funchook_prepare(httpHook, recv_func_point, staticCFunction(::drillRecv)).check("prepare recv_func_point")
    block()
    funchook_install(httpHook, 0).check("funchook_install")
}


fun removeHttpHook() {
    funchook_uninstall(httpHook, 0).check("funchook_uninstall")
}

fun tryDetectHttp(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, read: ssize_t) = memScoped {
    try {
        buf?.getPointer(this)?.readBytes(HTTP_DETECTOR_BYTES_COUNT)?.decodeToString()?.let { prefix ->
            val readBytesClb = { buf.getPointer(this).readBytes(read.convert()) }
            if (HTTP_VERBS.any { prefix.startsWith(it) }) {
                readBytesClb().let { readBytes -> processHttpRequest(readBytes, fd) { readBytes } }
            } else if (reader[fd] != null) {
                readBytesClb().let { readBytes ->
                    processHttpRequest(readBytes, fd) {
                        reader.remove(fd)?.plus(readBytes)
                    }
                }
            }
        }
    } catch (ex: Throwable) {
        ex.printStackTrace()
    }
}


private fun processHttpRequest(readBytes: ByteArray, fd: DRILL_SOCKET, dataCallback: (() -> ByteArray?)) =
    if (notContainsFullHeadersPart(readBytes)) {
        reader[fd] = reader[fd] ?: byteArrayOf() + readBytes
    } else {
        httpReadCallbacks.forEach { clb ->
            clb(dataCallback())
        }
    }


private fun notContainsFullHeadersPart(readBytes: ByteArray) = readBytes.indexOf(HEADERS_DELIMITER) == -1

fun MemScope.processWriteEvent(buf: CPointer<ByteVarOf<Byte>>?, size: Int): HttpData {
    try {
        buf?.readBytes(HTTP_DETECTOR_BYTES_COUNT)?.decodeToString()?.let { prefix ->
            if (HTTP_VERBS.any { prefix.startsWith(it) }) {
                val readBytes = buf.readBytes(size.convert())
                val index = readBytes.indexOf(CR_LF_BYTES)
                if (index > 0) {
                    val httpWriteHeaders = httpWriteCallback()
                    if (isNotContainsDrillHeaders(readBytes, httpWriteHeaders)) {
                        val firstLineOfResponse = readBytes.copyOfRange(FIRST_INDEX, index)
                        val injectedHeader = prepareHeaders(httpWriteHeaders)
                        val responseTail = readBytes.copyOfRange(index, size.convert())
                        val modified = firstLineOfResponse + injectedHeader + responseTail
                        return HttpData(modified.toCValues().getPointer(this), modified.size, injectedHeader.size)
                    }
                }
            }
        }
    } catch (ex: Exception) {
        log("Drill-hook got error", ex)
    }
    return HttpData(buf, size)
}

fun log(message: String, ex: Exception) {
    println("DEBUG: $message")
    ex.getStackTrace().forEach {
        println("\t$it")
    }

}

private fun prepareHeaders(httpWriteHeaders: Map<String, String>) =
    CR_LF_BYTES + httpWriteHeaders.map { (k, v) -> "$k: $v" }.joinToString(CR_LF).encodeToByteArray()

private fun isNotContainsDrillHeaders(readBytes: ByteArray, httpWriteHeaders: Map<String, String>) =
    httpWriteHeaders.isNotEmpty() && readBytes.indexOf(httpWriteHeaders.entries.first().key.encodeToByteArray()) == -1

data class HttpData(val buf: CPointer<ByteVarOf<Byte>>?, val size: Int, val dif: Int = 0)