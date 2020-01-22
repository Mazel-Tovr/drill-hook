package com.epam.drill.hook

fun ByteArray.indexOf(headerLineDelimiter: ByteArray) = this.run {
    for (index in indices) {
        if (index + headerLineDelimiter.size <= this.size) {
            val regionMatches = headerLineDelimiter.foldIndexed(true) { i, acc, byte ->
                acc && this[index + i] == byte
            }
            if (regionMatches) return@run index
        } else break
    }
    -1
}