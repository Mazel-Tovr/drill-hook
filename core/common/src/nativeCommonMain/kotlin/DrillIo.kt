@file:Suppress("ObjectPropertyName")

package com.epam.drill.hook

import com.epam.drill.hook.gen.DRILL_SOCKET
import com.epam.drill.hook.http.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.SharedImmutable


@SharedImmutable
val HTTP_VERBS = setOf("HTTP", "OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT")

@SharedImmutable
val HEADERS_DELIMITER = byteArrayOf(13, 10, 13, 10)

@SharedImmutable
val HEADER_LINE_DELIMITER = byteArrayOf(13, 10)

const val FIRST_INDEX = 0

internal fun drillRead(fd: Int, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()
    val read = nativeRead(fd, buf, size)
    memScoped {
        buf?.getPointer(this)?.readBytes(8)?.decodeToString()?.let { prefix ->
            if (HTTP_VERBS.any { prefix.startsWith(it) }) {
                val contentAsString =
                    buf.getPointer(this).readBytes(read.convert()).decodeToString(throwOnInvalidSequence = true)
                httpReadCallbacks.forEach {
                    it(contentAsString)
                }
            }
        }
    }
    return read.convert()
}

internal fun drillWrite(fd: Int, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()
    memScoped {
        buf?.getPointer(this)?.readBytes(8)?.decodeToString()?.let { prefix ->
            return if (prefix.startsWith("HTTP")) {
                val count = size.convert<Int>()
                val readBytes = buf.getPointer(this).readBytes(count)
                val index = readBytes.indexOf(HEADER_LINE_DELIMITER)
                val headers = httpWriteCallback?.invoke()
                if (index > 0 && headers != null) {
                    val firstLineOfResponse = readBytes.copyOfRange(FIRST_INDEX, index)

                    val injectedHeader = "\r\n${headers.map { (k, v) ->
                        "$k: $v"
                    }.joinToString("\r\n")}".encodeToByteArray()
                    val responseTail = readBytes.copyOfRange(index, count)
                    val modified = firstLineOfResponse + injectedHeader + responseTail
                    nativeWrite(fd, modified.toCValues().getPointer(this), modified.size.convert()).convert()
                } else
                    nativeWrite(fd, buf, size).convert()

            } else {
                nativeWrite(fd, buf, size).convert()
            }
        }
    }

    return nativeWrite(fd, buf, size).convert()
}

internal fun drillSend(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags:Int): Int {
    initRuntimeIfNeeded()
    memScoped {
        buf?.getPointer(this)?.readBytes(8)?.decodeToString()?.let { prefix ->
            return if (prefix.startsWith("HTTP")) {
                val count = size.convert<Int>()
                val readBytes = buf.getPointer(this).readBytes(count)
                val index = readBytes.indexOf(HEADER_LINE_DELIMITER)
                val headers = httpWriteCallback?.invoke()
                if (index > 0 && headers != null) {
                    val firstLineOfResponse = readBytes.copyOfRange(FIRST_INDEX, index)

                    val injectedHeader = "\r\n${headers.map { (k, v) ->
                        "$k: $v"
                    }.joinToString("\r\n")}".encodeToByteArray()
                    val responseTail = readBytes.copyOfRange(index, count)
                    val modified = firstLineOfResponse + injectedHeader + responseTail
                    nativeSend(fd, modified.toCValues().getPointer(this), modified.size.convert(), flags).convert()
                } else
                    nativeSend(fd, buf, size.convert(), flags).convert()

            } else {
                nativeSend(fd, buf, size.convert(),flags).convert()
            }
        }
    }

    return nativeSend(fd, buf, size.convert(),flags).convert()
}