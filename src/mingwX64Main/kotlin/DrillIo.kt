package com.epam.drill.hook.io

import com.epam.drill.hook.gen.funchook_prepare
import com.epam.drill.hook.gen.wsaSend_func
import com.epam.drill.hook.gen.wsaSend_func_point
import com.epam.drill.hook.gen.wsaRecv_func
import com.epam.drill.hook.gen.wsaRecv_func_point
import com.epam.drill.hook.io.tcp.tryDetectProtocol
import kotlinx.cinterop.*
import platform.posix.LPWSAOVERLAPPED_COMPLETION_ROUTINE
import platform.posix.SOCKET
import platform.posix.WSABUF
import platform.posix._WSABUF
import platform.windows.LPDWORD
import platform.windows._OVERLAPPED

fun configureTcpHooks() {
    configureTcpHooksBuild {
        println("Configuration for mingw")
        funchook_prepare(tcpHook, wsaSend_func_point, staticCFunction {
            fd: SOCKET,
            buff: CPointer<_WSABUF>?,
            buffersSize: UInt,
            written: LPDWORD?,
            p5: UInt,
            p6: CPointer<_OVERLAPPED>?,
            p7: LPWSAOVERLAPPED_COMPLETION_ROUTINE?
            ->
            initRuntimeIfNeeded()
            var dif = 0
            memScoped<Int> {
                for (i in 0 until buffersSize.toInt()) {
                    val buffer = buff!![i]
                    val size = buffer.len
                    val buf = buffer.buf
                    val (finalBuf, finalSize, injectedSize) = processWriteEvent(fd.convert(), buf, size.convert())
                    dif += injectedSize
                    buff[i].buf = finalBuf
                    buff[i].len = finalSize.convert()
                }
                val wsasendFunc = wsaSend_func!!(fd, buff, buffersSize, written, p5, p6, p7)
                written!!.pointed.value -= dif.toUInt()
                (wsasendFunc).convert()

        }})
        funchook_prepare(tcpHook, wsaRecv_func_point, staticCFunction {

            fd: SOCKET,
            buff: CPointer<_WSABUF>?,
            p3: UInt,
            read: LPDWORD?,
            p5: LPDWORD?,
            p6: CPointer<_OVERLAPPED>?,
            p7: LPWSAOVERLAPPED_COMPLETION_ROUTINE?
            ->
            initRuntimeIfNeeded()
            val wsarecvFunc: Int = wsaRecv_func!!(fd, buff, p3, read, p5, p6, p7)
            val finalBuf = buff!!.pointed
            tryDetectProtocol(fd, finalBuf.buf, read!!.pointed.value.convert())
            wsarecvFunc
        })
    }
}

