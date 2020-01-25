@file:Suppress("ObjectPropertyName")

package com.epam.drill.hook

import com.epam.drill.hook.gen.*
import com.epam.drill.hook.http.HttpData
import com.epam.drill.hook.http.processWriteEvent
import com.epam.drill.hook.http.tryDetectHttp
import kotlinx.cinterop.*
import platform.posix.size_t
import platform.posix.sockaddr
import platform.posix.ssize_t
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

@Suppress("unused")
@kotlin.native.concurrent.SharedImmutable
val tcpInitializer = run {
    val socketHook = funchook_create()
    funchook_prepare(socketHook, close_func_point, staticCFunction(::drillClose)).check("prepare close_func_point")
    funchook_prepare(
        socketHook,
        connect_func_point,
        staticCFunction(::drillConnect)
    ).check("prepare connect_func_point")
    funchook_prepare(socketHook, accept_func_point, staticCFunction(::drillAccept)).check("prepare accept_func_point")
    funchook_install(socketHook, 0).check("funchook_install")
}

@SharedImmutable
private val accessThread = Worker.start(true)

@ThreadLocal
private val _connects: MutableSet<DRILL_SOCKET> = mutableSetOf()
@ThreadLocal
private val _accepts: MutableSet<DRILL_SOCKET> = mutableSetOf()

val connects
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _connects }.result

val accepts
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _accepts }.result




internal fun drillRead(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()
    val read = nativeRead(fd.convert(), buf, size.convert())
    if (read > 0 && (accepts.contains(fd.convert()) || connects.contains(fd.convert())))
        tryDetectHttp(fd.convert(), buf, read.convert())
    return read.convert()
}


internal fun drillWrite(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()

    return memScoped {
        val (finalBuf, finalSize, ll) =
            if (accepts.contains(fd.convert()) || connects.contains(fd.convert()))
                processWriteEvent(buf, size.convert())
            else HttpData(buf, size.convert())
        (nativeWrite(fd.convert(), finalBuf, (finalSize).convert()) - ll).convert()
    }
}

internal fun drillSend(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int {
    initRuntimeIfNeeded()
    return memScoped {
        val (finalBuf, finalSize, ll) = processWriteEvent(buf, size.convert())
        (nativeSend(fd.convert(), finalBuf, (finalSize).convert(), flags) - ll).convert()
    }
}

internal fun drillRecv(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int {
    initRuntimeIfNeeded()
    if(size < 0 ){
        println("WTF!!?!?!>!>!?!>>!")
    }
    val read = nativeRecv(fd, buf, size.convert(), flags)
    tryDetectHttp(fd, buf, read.convert())
    return read.convert()
}

internal fun drillConnect(fd: DRILL_SOCKET, addr: CPointer<sockaddr>?, socklen: drill_sock_len): Int {
    initRuntimeIfNeeded()
    val connectStatus = nativeConnect(fd, addr, socklen).convert<Int>()
    if (0 == connectStatus) connects += fd
    return connectStatus
}

internal fun drillAccept(
    fd: DRILL_SOCKET,
    addr: CPointer<sockaddr>?,
    socklen: CPointer<drill_sock_lenVar>?
): DRILL_SOCKET {
    initRuntimeIfNeeded()
    val socket = nativeAccept(fd, addr, socklen)
    if (isValidSocket(socket) == 0)
        accepts += socket
    return socket
}

internal fun drillClose(fd: DRILL_SOCKET): Int {
    initRuntimeIfNeeded()
    val result = nativeClose(fd)
    if (result == 0) {
        accepts -= fd
        connects -= fd
    }
    return result
}