package com.epam.drill.hook.io

import com.epam.drill.hook.gen.*
import com.epam.drill.hook.io.tcp.processWriteEvent
import kotlinx.cinterop.*
import platform.posix.iovec
import platform.posix.ssize_t

fun configureTcpHooks() = configureTcpHooksBuild {
    println("Configuration for unix")
    funchook_prepare(tcpHook, writev_func_point, staticCFunction(::writevDrill))
    funchook_prepare(tcpHook, readv_func_point, staticCFunction(::readvDrill))
}

fun readvDrill(fd: Int, iovec: CPointer<iovec>?, size: Int): ssize_t {
    val convert = readv_func!!(fd, iovec, size).convert<ssize_t>()
    println("readv intercepted do not implemented now. If you see this message, please put issue to https://github.com/Drill4J/Drill4J")
    return convert
}


fun writevDrill(fd: Int, iovec: CPointer<iovec>?, size: Int): ssize_t {
    return memScoped {
        //todo I think we(headers) should be in the first buffer
        val iovecs = iovec!![0]
        val iovLen = iovecs.iov_len
        val base = iovecs.iov_base!!.reinterpret<ByteVarOf<Byte>>()
        val (finalBuf, finalSize, injectedSize) = processWriteEvent(fd.convert(), base, iovLen.convert())
        iovec[0].iov_base = finalBuf
        iovec[0].iov_len = finalSize.convert()
        (writev_func!!(fd, iovec, size) - injectedSize).convert()

    }

}