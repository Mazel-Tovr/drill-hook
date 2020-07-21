package com.epam.drill.hook.io.tcp

import com.epam.drill.hook.gen.DRILL_SOCKET
import com.epam.drill.hook.io.TcpFinalData
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.memScoped
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlin.native.concurrent.freeze


@SharedImmutable
private val _interceptors = atomic(persistentListOf<Interceptor>())

val interceptors: List<Interceptor>
    get() = _interceptors.value

fun addInterceptor(interceptor: Interceptor) {
    _interceptors.update { it + interceptor }
}

interface ReadInterceptor {
    fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int)
}

interface WriteInterceptor {
    fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int): TcpFinalData
}

interface Interceptor : ReadInterceptor, WriteInterceptor {
    fun isSuitableByteStream(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>): Boolean
    fun close(fd: DRILL_SOCKET)
}


fun tryDetectProtocol(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int) {
    buf?.let { byteBuf ->
        interceptors.forEach {
            it.let {
                if (it.isSuitableByteStream(fd, byteBuf)) {
                    memScoped {
                        with(it) {
                            interceptRead(fd, buf, size)
                        }
                    }
                }

            }
        }
    }
}

fun close(fd: DRILL_SOCKET) {
    interceptors.forEach {
        it.close(fd)
    }
}

fun MemScope.processWriteEvent(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int): TcpFinalData {
    return buf?.let { byteBuf ->
        interceptors.forEach {
            it.let {
                if (it.isSuitableByteStream(fd, byteBuf))
                    return with(it) {
                        interceptWrite(fd, buf, size)
                    }
                else TcpFinalData(buf, size)
            }
        }
        TcpFinalData(buf, size)
    } ?: TcpFinalData(buf, size)


}

@SharedImmutable
val CR_LF = "\r\n"

@SharedImmutable
val CR_LF_BYTES = CR_LF.encodeToByteArray()

@SharedImmutable
val HEADERS_DELIMITER = CR_LF_BYTES + CR_LF_BYTES

@SharedImmutable
val injectedHeaders = atomic({ emptyMap<String, String>() }.freeze()).freeze()

@SharedImmutable
val readHeaders = atomic({ _: Map<ByteArray, ByteArray> -> Unit }.freeze()).freeze()

@SharedImmutable
val readCallback = atomic({ _: ByteArray -> Unit }.freeze()).freeze()

@SharedImmutable
val writeCallback = atomic({ _: ByteArray -> Unit }.freeze()).freeze()