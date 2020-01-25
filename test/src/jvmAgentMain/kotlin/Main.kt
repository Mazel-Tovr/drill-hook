@file:Suppress("UNUSED_PARAMETER")

import com.epam.drill.hook.http.addHttpReadCallbacks
import com.epam.drill.hook.http.addHttpWriteCallback
import com.epam.drill.hook.http.configureHttpHooks
import com.epam.drill.hook.http.removeHttpHook
import com.epam.drill.hook.indexOf
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: Long, options: String, reservedPtr: Long): Int {
    return 0
}

@Suppress("unused")
@CName("Java_bindings_Bindings_removeHttpHook")
fun removeHttpHook(env: JNIEnv, thiz: jobject) {
    removeHttpHook()
}

@Suppress("unused")
@CName("Java_bindings_Bindings_addHttpHook")
fun addHttpHook(env: JNIEnv, thiz: jobject) {

    configureHttpHooks()

    addHttpReadCallbacks {
        println(it?.contentToString())
    }

    addHttpWriteCallback { injectedHeaders }
}