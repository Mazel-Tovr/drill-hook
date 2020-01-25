import io.undertow.Undertow
import io.undertow.util.Headers
import java.net.InetSocketAddress

class UndertowTest : TestBase() {

    override fun setupServer() {
        val server = Undertow.builder()
            .addHttpListener(0, "localhost")
            .setHandler { exchange ->
                exchange.responseHeaders.put(Headers.CONTENT_TYPE, "text/plain")
                exchange.responseSender.send(responseMessage)
            }.build()
        server.start()
        port = (server.listenerInfo.first().address as InetSocketAddress).port
    }

}