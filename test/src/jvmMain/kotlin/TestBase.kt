import bindings.Bindings
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class TestBase {
    var port: Int = 0

    @BeforeTest
    abstract fun setupServer()

    @Test
    fun `should add and remove hooks`() {
        val address = "http://localhost:$port"
        doHttpCall(address)
        Bindings.addHttpHook()
        val (key, value) = injectedHeaders.asSequence().first()

        doHttpCall(address).let { (headers, body) ->
            assertEquals(responseMessage, body.trim())
            assertEquals(value, headers[key]?.first())
        }

        Bindings.removeHttpHook()
        doHttpCall(address).let { (headers, body) ->
            assertEquals(responseMessage, body.trim())
            assertEquals(null, headers[key]?.first())
        }
    }

}

fun doHttpCall(address: String): Pair<MutableMap<String, MutableList<String>>, String> {
    var connection: HttpURLConnection? = null
    try {
        val url = URL(address)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Language", "en-US")
        connection.useCaches = false
        connection.doOutput = true
        val wr = DataOutputStream(connection.outputStream)
        wr.close()

        val `is` = connection.inputStream
        val rd = BufferedReader(InputStreamReader(`is`))
        val response = StringBuilder() // or StringBuffer if Java version 5+
        var line: String?
        while (rd.readLine().also { line = it } != null) {
            response.append(line)
            response.append('\r')
        }
        rd.close()
        return connection.headerFields to response.toString()
    } catch (e: Exception) {

        e.printStackTrace()
        fail()
    } finally {
        connection?.disconnect()
    }
}
