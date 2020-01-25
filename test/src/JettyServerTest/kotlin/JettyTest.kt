import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets.UTF_8
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class JettyTest : TestBase() {

    override fun setupServer() {
        val server = Server()
        val connector = ServerConnector(server)
        connector.port = 0
        server.connectors = arrayOf(connector)
        server.handler = object : AbstractHandler() {
            @Throws(IOException::class, ServletException::class)
            override fun handle(
                target: String,
                baseRequest: Request,
                request: HttpServletRequest?,
                response: HttpServletResponse
            ) {
                baseRequest.isHandled = true
                response.contentType = "text/plain"
                val content = responseMessage
                response.setContentLength(content.length)
                val outputStream = response.outputStream
                val writer = OutputStreamWriter(outputStream, UTF_8)
                writer.write(content)
                writer.flush()
            }
        }
        server.start()

        port = (server.connectors.first() as ServerConnector).localPort
    }

}