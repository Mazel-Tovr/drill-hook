import kotlin.native.concurrent.SharedImmutable

const val responseMessage = "my message"

@SharedImmutable
val injectedHeaders = mapOf("xxx" to "yyy")