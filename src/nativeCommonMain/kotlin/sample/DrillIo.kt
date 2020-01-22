package com.epam.drill.hook

import com.epam.drill.hook.gen.read_func
import com.epam.drill.hook.gen.write_func
import kotlinx.cinterop.*
import platform.posix.size_t
import platform.posix.ssize_t


@SharedImmutable
val HTTP_VERBS = setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT")


fun drill_read(fd: Int, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    val read = read_func!!(fd, buf, size)
    memScoped {
        buf?.getPointer(this)?.readBytes(8)?.decodeToString()?.let { prefix ->
            if (HTTP_VERBS.any { prefix.startsWith(it) }) {
                val contentAsString =
                    buf.getPointer(this).readBytes(read.toInt()).decodeToString(throwOnInvalidSequence = true)
                val n = contentAsString.indexOf('\n')
//                if (n != -1)
            }
        }
    }
    return read
}

fun drill_write(
    fd: Int,
    buf: CPointer<ByteVarOf<Byte>>?,
    size: size_t /* = kotlin.ULong */
): ssize_t /* = kotlin.Long */ {
    initRuntimeIfNeeded()
    memScoped {
        //
        buf?.getPointer(this)?.readBytes(8)?.decodeToString()?.let { prefix ->

            return if (prefix.startsWith("HTTP")) {
                val readBytes = buf.getPointer(this).readBytes(size.toInt())

//                puts((w + "").replace("200", "400") + "\n")
//                val replace = w?.replaceFirst("200", "400")
//                val encodeToByteArray = replace?.encodeToByteArray()!!

                val p2 = readBytes.decodeToString().replaceFirst("\r\n", "\r\nxxx: xxx\r\n")
                write_func!!(fd, p2.encodeToByteArray().toCValues().getPointer(this), p2.encodeToByteArray().size.toULong())

            } else {
                write_func!!(fd, buf, size)
            }
        }
    }

    return write_func!!(fd, buf, size)
}