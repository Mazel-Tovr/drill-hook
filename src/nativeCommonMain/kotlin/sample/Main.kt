package com.epam.drill.hook

import com.epam.drill.hook.gen.*
//import com.epam.drill.hook.gen.winsend_hook
//import com.epam.drill.hook.gen.wsock
import kotlinx.cinterop.*
import platform.posix.*

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: Long, options: String, reservedPtr: Long): Int {
    try {
        main()
    } catch (ex: Throwable) {
        println("Can't load the agent. Ex: ${ex.message}")
    }
    return 0
}


fun main() {
    val fileName = "test-1.txt"

    create_funchook()
    read_hook(staticCFunction(::drill_read).reinterpret())
    write_hook(staticCFunction(::drill_write).reinterpret())
//    winsend_hook(staticCFunction { fd, buf, size, x4, x5, x6, x7 ->
//        initRuntimeIfNeeded()
//
//        memScoped {
//            val reinterpret = buf?.reinterpret<WSABUF>()
//            val pointed = reinterpret?.pointed
//            val len = pointed?.len
//            val buf1 = pointed?.buf
//            println("buff: $buf1")
//            println("len: $len")
//            val bytes = buf1?.getPointer(this)?.readBytes(len!!.toInt())
//            bytes?.let { prefix ->
//                println("contentToString: ${bytes.contentToString()}")
//                println("contentToString-second: ${bytes.contentToString()}")
//
//                if (len!!.toInt() == 75) {
//                    println(
//                        "stroka normalnogo chekoveka: ${byteArrayOf(
//                            72,
//                            84,
//                            84,
//                            80,
//                            47,
//                            49,
//                            46,
//                            49,
//                            32,
//                            50,
//                            48,
//                            48,
//                            32,
//                            79,
//                            75,
//                            13
//                        ).decodeToString()}"
//                    )
//                    println("content: ${bytes.copyOf(15).decodeToString()}")
//                }
//
//            }
////println("XXXX");
////                if (prefix.startsWith("HTTP")) {
//
////                    val readBytes = buf.getPointer(this).readBytes(size.toInt())
////
//////                println((w + "").replace("200", "400") + "\n")
//////                val replace = w?.replaceFirst("200", "400")
//////                val encodeToByteArray = replace?.encodeToByteArray()!!
////
////                    val p2 = readBytes.decodeToString().replaceFirst("\r\n", "\r\nxxx: xxx\r\n")
////                    val p21 = p2.encodeToByteArray().toCValues().getPointer(this)
////                    wsock!!(fd, p21.reinterpret(), p2.encodeToByteArray().size.toUInt(), x4, x5, x6, x7)
////
////
////                } else {
////                    wsock!!(fd, buf, size, x4, x5, x6, x7)
////                }
////            }
//        }
//
//        wsock!!(fd, buf, size, x4, x5, x6, x7)
//
//    })
    install_funchooks()

    memScoped {
        val fd = open(fileName, O_WRONLY)
        val message = "HTTP1.0 200 OK\nheader1: value1\r\n\r\n".encodeToByteArray()
        write(fd, message.toCValues(), message.size.convert())
        val bufferLength = 100L
        val buffer = allocArray<ByteVar>(bufferLength)
        val read = read(open(fileName, O_RDONLY), buffer, bufferLength.convert())
        println(buffer.getPointer(this).readBytes(read.convert()).decodeToString())
        close(fd)
    }


}