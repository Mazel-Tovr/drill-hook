package com.epam.drill.hook.io.tcp

import co.touchlab.stately.collections.sharedMutableSetOf
import com.epam.drill.hook.gen.DRILL_SOCKET
import com.epam.drill.hook.io.TcpFinalData
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.memScoped
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze


@SharedImmutable
val interceptors =sharedMutableSetOf<Interceptor>()

interface ReadInterceptor {
    fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int)
}

interface WriteInterceptor {
    fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int): TcpFinalData
}

interface Interceptor : ReadInterceptor, WriteInterceptor {
    fun isSuitableByteStream(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>): Boolean
}


fun tryDetectProtocol(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int) {
    buf?.let { byteBuf ->
        interceptors.forEach {
            it?.let {
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


fun MemScope.processWriteEvent(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int): TcpFinalData {
    return buf?.let { byteBuf ->
        interceptors.forEach {
            it.let {
                if (it.isSuitableByteStream(fd, byteBuf))
                    return  with(it) {
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
val headersForInject = AtomicReference({ emptyMap<String, String>() }.freeze()).freeze()

@SharedImmutable
val readCallback = AtomicReference({ _: ByteArray -> Unit }.freeze()).freeze()

@SharedImmutable
val writeCallback = AtomicReference({ _: ByteArray -> Unit }.freeze()).freeze()