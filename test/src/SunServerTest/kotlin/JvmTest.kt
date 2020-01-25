import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress


class JvmTest : TestBase() {

    override fun setupServer() {
        val server = HttpServer.create()
        server.bind(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            generateBigSizeHeaders(exchange)
            val bytes = responseMessage.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            val os = exchange.responseBody
            os.write(bytes)
            os.close()
        }
        server.start()
        port = server.address.port
    }

    private fun generateBigSizeHeaders(exchange: HttpExchange) {
        repeat(30000) {
            exchange.responseHeaders["header$it"] = listOf("any")
        }
    }

}