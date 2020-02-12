@file:Suppress("ObjectPropertyName")

package com.epam.drill.hook.io

import com.epam.drill.hook.gen.*
import com.epam.drill.hook.io.tcp.processWriteEvent
import com.epam.drill.hook.io.tcp.tryDetectProtocol
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

@ThreadLocal
private var _tcpHook: CPointer<funchook_t>? = null

var tcpHook
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _tcpHook }.result
    set(value) = accessThread.execute(TransferMode.UNSAFE, { value }) { _tcpHook = it }.result


fun configureTcpHooksBuild(block: () -> Unit) = if (tcpHook != null) {
    funchook_install(tcpHook, 0).check("funchook_install")
} else {
    tcpHook = funchook_create() ?: run {
        println("Failed to create hook")
        return
    }
    funchook_prepare(tcpHook, read_func_point, staticCFunction(::drillRead)).check("prepare read_func_point")
    funchook_prepare(tcpHook, write_func_point, staticCFunction(::drillWrite)).check("prepare write_func_point")
    funchook_prepare(tcpHook, send_func_point, staticCFunction(::drillSend)).check("prepare send_func_point")
    funchook_prepare(tcpHook, recv_func_point, staticCFunction(::drillRecv)).check("prepare recv_func_point")
    block()
    funchook_install(tcpHook, 0).check("funchook_install")
}


fun removeTcpHook() {
    funchook_uninstall(tcpHook, 0).check("funchook_uninstall")
}

val connects
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _connects }.result

val accepts
    get() = accessThread.execute(TransferMode.UNSAFE, {}) { _accepts }.result


internal fun drillRead(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()
    val read = nativeRead(fd.convert(), buf, size.convert())
    if (read > 0 && (accepts.contains(fd.convert()) || connects.contains(fd.convert())))
        tryDetectProtocol(fd.convert(), buf, read.convert())
    return read.convert()
}

internal fun drillWrite(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    initRuntimeIfNeeded()

    return memScoped {
        val (finalBuf, finalSize, ll) =
                if (accepts.contains(fd.convert()) || connects.contains(fd.convert()))
                    processWriteEvent(fd.convert(), buf, size.convert())
                else TcpFinalData(buf, size.convert())
        (nativeWrite(fd.convert(), finalBuf, (finalSize).convert()) - ll).convert()
    }
}


internal fun drillSend(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int {
    initRuntimeIfNeeded()
    return memScoped {
        val (finalBuf, finalSize, ll) = processWriteEvent(fd, buf, size.convert())
        (nativeSend(fd.convert(), finalBuf, (finalSize).convert(), flags) - ll).convert()
    }
}

internal fun drillRecv(fd: DRILL_SOCKET, buf: CPointer<ByteVarOf<Byte>>?, size: Int, flags: Int): Int {
    initRuntimeIfNeeded()
    val read = nativeRecv(fd, buf, size.convert(), flags)
    tryDetectProtocol(fd, buf, read.convert())
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