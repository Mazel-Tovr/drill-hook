import com.epam.drill.hook.http.*

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: Long, options: String, reservedPtr: Long): Int {
    try {
        configureHttpHooks()
        addHttpReadCallbacks {
            println("Got it: $it")
        }
        addHttpWriteCallback {
            mapOf("xxx" to "yyy")
        }
    } catch (ex: Throwable) {
        ex.printStackTrace()
        println("Can't load the agent. Ex: ${ex.message}")
    }
    return 0
}