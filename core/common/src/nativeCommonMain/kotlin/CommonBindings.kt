package com.epam.drill.hook
import com.epam.drill.hook.gen.*

val nativeRead
    get() = read_func!!

val nativeWrite
    get() = write_func!!

val nativeSend
    get() = send_func!!
