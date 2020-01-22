package com.epam.drill.hook.http

actual fun configureHttpHooks() = configureHttpHooksBuild {
    println("Configuration for mingw")
}
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
