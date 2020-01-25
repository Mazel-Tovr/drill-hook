package com.epam.drill.hook

fun ByteArray.indexOf(arr: ByteArray) = run {
    for (index in indices) {
        if (index + arr.size <= this.size) {
            val regionMatches = arr.foldIndexed(true) { i, acc, byte ->
                acc && this[index + i] == byte
            }
            if (regionMatches) return@run index
        } else break
    }
    -1
}