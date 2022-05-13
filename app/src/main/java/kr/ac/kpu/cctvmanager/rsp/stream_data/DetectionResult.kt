package kr.ac.kpu.cctvmanager.rsp.stream_data

data class DetectionResult(val timestamp: Int, val boxes: List<DetectionBox>) : StreamData {
    data class DetectionBox(
        val left: Int,
        val right: Int,
        val top: Int,
        val bottom: Int,
        val confidence: Double,
        val classID: Int,
        val label: String)

    override fun getMessageString(): String {
        val boxLines = boxes.map { box ->
                    "${box.left}," +
                    "${box.right}," +
                    "${box.top}," +
                    "${box.bottom}," +
                    "${box.confidence}," +
                    "${box.classID}," +
                    "${box.label}\n"}

        return "$timestamp\n" +
                boxLines.joinToString(separator="")
    }
}
