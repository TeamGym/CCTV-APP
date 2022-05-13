package kr.ac.kpu.cctvmanager.rsp.stream_data

data class ControlPTZ(val pan: Int,
                      val tilt: Int,
                      val zoom: Int): StreamData {
    override fun getMessageString(): String {
        return "$pan," +
                "$tilt," +
                "$zoom\n"
    }
}
