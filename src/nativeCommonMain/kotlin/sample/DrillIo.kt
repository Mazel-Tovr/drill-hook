package com.epam.drill.hook

import com.epam.drill.hook.gen.*
import kotlinx.cinterop.*
import platform.posix.*


@SharedImmutable
val HTTP_VERBS = setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT")


fun drill_read(fd: Int, buf: CPointer<ByteVarOf<Byte>>?, size: size_t): ssize_t {
    val read = read_func!!(fd, buf, size)
    memScoped {
        buf?.getPointer(this)?.readBytes(8)?.decodeToString()?.let { prefix ->
            if (HTTP_VERBS.any { prefix.startsWith(it) }) {
                val contentAsString =
                    buf.getPointer(this).readBytes(read.convert()).decodeToString(throwOnInvalidSequence = true)
                val n = contentAsString.indexOf('\n')
//                if (n != -1)
            }
        }
    }
    return read.convert()
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
                val readBytes = buf.getPointer(this).readBytes(size.convert())

                val p2 = readBytes.decodeToString().replaceFirst("\r\n", "\r\ncustom-header: custom-value\r\n")
                write_func!!(
                    fd,
                    p2.encodeToByteArray().toCValues().getPointer(this),
                    p2.encodeToByteArray().size.convert()
                ).convert()

            } else {
                write_func!!(fd, buf, size).convert()
            }
        }
    }

    return write_func!!(fd, buf, size).convert()
}