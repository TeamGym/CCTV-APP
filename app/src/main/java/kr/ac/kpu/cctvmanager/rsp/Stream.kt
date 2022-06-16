package kr.ac.kpu.cctvmanager.rsp

import kr.ac.kpu.cctvmanager.rsp.stream_data.StreamData

class Stream(val channel: Int,
             val streamType: Type,
             var data: StreamData? = null) : Message() {

    override fun getMessageString(): String {
        return "S$channel ${streamType.typeNumber}\n" +
                data?.getMessageString() +
                "\n"
    }

    enum class Type(val typeNumber: Int) {
        DETECTION_RESULT(1),
        CONTROL_PTZ(2);

        companion object {
            fun fromTypeNumber(typeNumber: Int) =
                values().associateBy(Type::typeNumber)[typeNumber]
        }
    }
}