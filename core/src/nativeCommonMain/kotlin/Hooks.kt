package com.epam.drill.hook

import com.epam.drill.hook.gen.*

val nativeRead
    get() = read_func!!

val nativeWrite
    get() = write_func!!

val nativeSend
    get() = send_func!!

val nativeRecv
    get() = recv_func!!

val nativeConnect
    get() = connect_func!!

val nativeAccept
    get() = accept_func!!

val nativeClose
    get() = close_func!!


@Suppress("NOTHING_TO_INLINE")
inline fun Int.check(message: String) {
    if(this < 0)
        println("Hook operation '$message' failed")
    return
}