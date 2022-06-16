package kr.ac.kpu.cctvmanager.rsp.parser

import kr.ac.kpu.cctvmanager.rsp.Stream
import kr.ac.kpu.cctvmanager.rsp.stream_data.parser.StreamDataParser
import kr.ac.kpu.cctvmanager.rsp.stream_data.parser.ControlPTZParser
import kr.ac.kpu.cctvmanager.rsp.stream_data.parser.DetectionResultParser

class StreamParser : MessageParser<StreamParser.State, Stream>(State.READY) {
    var dataParser: StreamDataParser<*, *>? = null
        private set

    override fun parseLine(line: String): Pair<State, Stream?> {
        when (state) {
            State.READY -> {
                if (!line.startsWith("S"))
                    return returnState(State.FAILED)

                val tokens = line.substring(1).split(" ")

                if (tokens.size != 2)
                    return returnState(State.FAILED)

                val channel = tokens[0].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val streamTypeNumber = tokens[1].toIntOrNull()
                    ?: return returnState(State.FAILED)

                val streamType = Stream.Type.fromTypeNumber(streamTypeNumber)
                    ?: return returnState(State.FAILED)

                content = Stream(channel, streamType)

                dataParser = when (streamType) {
                    Stream.Type.DETECTION_RESULT -> DetectionResultParser()
                    Stream.Type.CONTROL_PTZ -> ControlPTZParser()
                }
                state = State.PARSE_DATA
            }
            State.PARSE_DATA -> {
                val (state, data) = dataParser!!.parseLine(line)

                if (state.isFailed())
                    return returnState(State.FAILED)

                if (state.isDone()) {
                    content!!.data = data
                    return returnState(State.DONE, content)
                }
            }
            else -> throw AssertionError()
        }

        return state to null
    }

    enum class State(val value: Int) : Parser.State {
        READY(0),
        PARSE_DATA(1),
        DONE(100),
        FAILED(200);

        override fun isTerminated() = value >= 100
        override fun isDone() = this == DONE
        override fun isFailed() = value >= 200
    }
}