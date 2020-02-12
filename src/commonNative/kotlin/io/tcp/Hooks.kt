package com.epam.drill.hook.io.tcp

import com.epam.drill.hook.gen.DRILL_SOCKET
import com.epam.drill.hook.io.TcpFinalData
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.memScoped
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze


@SharedImmutable
val interceptor = AtomicReference<Interceptor?>(null).freeze()


interface ReadInterceptor {
    fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int)
}

interface WriteInterceptor {
    fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int): TcpFinalData
}

interface Interceptor : ReadInterceptor, WriteInterceptor {
    fun isSuitableByteStream(bytes: CPointer<ByteVarOf<Byte>>): Boolean
}


fun tryDetectProtocol(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int) {
    buf?.let { byteBuf ->
        interceptor.value?.let {
            if (it.isSuitableByteStream(byteBuf)) {
                memScoped {
                    with(it) {
                        interceptRead(fd, buf, size)
                    }
                }
            }
        }
    }
}


fun MemScope.processWriteEvent(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int): TcpFinalData {
    return buf?.let { byteBuf ->
        interceptor.value?.let {
            if (it.isSuitableByteStream(byteBuf))
                with(it) {
                    interceptWrite(fd, buf, size)
                }
            else TcpFinalData(buf, size)
        }
    } ?: TcpFinalData(buf, size)


}
