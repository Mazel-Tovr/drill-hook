import com.epam.drill.hook.http.addHttpReadCallbacks
import com.epam.drill.hook.http.addHttpWriteCallback
import com.epam.drill.hook.http.configureHttpHooksBuild
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonTest {
    private val PROTOCOL = "HTTP1.0"
    private val HTTP_MESSAGE = "$PROTOCOL 200 OK\r\nheader1: value1\r\n\r\n".encodeToByteArray()

    @Ignore//work only with INET descriptors
    @Test
    fun test() {
        configureHttpHooksBuild {}
        addHttpReadCallbacks {
        }
        val headerPair = "myHeadr1" to "myValue1"
        val headerToInject = mapOf(headerPair)
        addHttpWriteCallback { headerToInject }
        memScoped {
            val tmpfile = tmpfile()
            val fd = fileno(tmpfile)
            repeat(100) {
                rewind(tmpfile)
                val write = write(fd, HTTP_MESSAGE.toCValues(), HTTP_MESSAGE.size.convert())
                assertTrue { write > 0 }
                rewind(tmpfile)
                val bufferLength = 100L
                val buffer = allocArray<ByteVar>(bufferLength)
                val read = read(fd, buffer, bufferLength.convert())
                assertTrue { read > 0 }
                val pointer = buffer.getPointer(this)
                val readBytes = pointer.readBytes(read.convert())
                val (key, value) = headerPair
                assertEquals("$key: $value", readBytes.decodeToString().lines()[1])
            }
        }


    }


}