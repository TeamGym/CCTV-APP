package kr.ac.kpu.cctvmanager.rsp

class Request(val method: Method,
              properties: Map<String, String> = mapOf(),
              val responseCallback: ((Response) -> Unit)? = null) : Message() {
    var delivered = false
    var sequence: UInt = 0U

    private var properties: MutableMap<String, String> = mutableMapOf(*properties.toList().toTypedArray())

    fun addProperty(name: String, value: String) {
        properties[name] = value
    }

    fun removeProperty(name: String) {
        properties.remove(name)
    }

    fun getPropertyKeys() = properties.keys

    override fun getMessageString(): String {
        val requestLine = "${method.name} RSP/$RSP_VERSION\n"
        val sequenceLine = "Seq=$sequence\n"
        val propertyLines = properties.map { (name, value) -> "$name=$value\n" }

        return requestLine +
                sequenceLine +
                propertyLines.joinToString(separator="") +
                "\n"
    }

    enum class Method {
        GET_INFO,
        JOIN,
        CONTROL_AUDIO
    }
}