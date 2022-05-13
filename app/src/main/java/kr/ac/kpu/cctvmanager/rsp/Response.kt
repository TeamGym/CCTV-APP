package kr.ac.kpu.cctvmanager.rsp

class Response(val statusCode: Int,
               initProperties: Map<String, String> = mapOf()) : Message() {
    var sequence: UInt = 0U

    private var properties: MutableMap<String, String> = mutableMapOf(*initProperties.toList().toTypedArray())

    fun addProperty(name: String, value: String) {
        properties[name] = value
    }

    fun removeProperty(name: String) {
        properties.remove(name)
    }

    fun getPropertyKeys() = properties.keys

    override fun getMessageString(): String {
        val responseLine = "RSP/$RSP_VERSION $statusCode\n"
        val sequenceLine = "Seq=$sequence\n"
        val propertyLines = properties.map { (name, value) -> "$name=$value\n" }

        return responseLine +
                sequenceLine +
                propertyLines.joinToString(separator="") +
                "\n"
    }

}