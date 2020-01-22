import com.sun.net.httpserver.*
import java.io.*
import java.net.*
import kotlin.test.*


const val response = "lol you"

class JvmTest {

    var port: Int = 0

    @BeforeTest
    fun configureServer() {
        val server: HttpServer = HttpServer.create()
        server.bind(InetSocketAddress(0), 0)
        server.createContext("/", ResponseHandler())
        server.start()
        port = server.address.port
    }

    @Test
    fun test() {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("http://localhost:$port")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )

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
            println(connection.headerFields)
            println(response.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }
}

class ResponseHandler : HttpHandler {
    @Throws(IOException::class)
    override fun handle(exchange: HttpExchange) {
        val bytes = response.toByteArray()
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        val os = exchange.responseBody
        os.write(bytes)
        os.close()
    }
}
