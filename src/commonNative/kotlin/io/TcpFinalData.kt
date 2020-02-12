package com.epam.drill.hook.io

import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer

data class TcpFinalData(val buf: CPointer<ByteVarOf<Byte>>?, val size: Int, val dif: Int = 0)