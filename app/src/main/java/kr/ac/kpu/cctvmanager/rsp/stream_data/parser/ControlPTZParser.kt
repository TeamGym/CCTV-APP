package kr.ac.kpu.cctvmanager.rsp.stream_data.parser

import kr.ac.kpu.cctvmanager.rsp.parser.Parser
import kr.ac.kpu.cctvmanager.rsp.stream_data.ControlPTZ

class ControlPTZParser
    : StreamDataParser<ControlPTZParser.State, ControlPTZ>(State.READY) {
    override fun parseLine(line: String): Pair<State, ControlPTZ?> {
        when (state) {
            State.READY -> {
                if (line.isEmpty())
                    return returnState(State.FAILED)

                val tokens = line.split(",")

                if (tokens.size != 3)
                    return returnState(State.FAILED)

                val pan = tokens[0].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val tilt = tokens[1].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val zoom = tokens[2].toIntOrNull()
                    ?: return returnState(State.FAILED)

                content = ControlPTZ(
                    pan=pan,
                    tilt=tilt,
                    zoom=zoom)
                state = State.READ_NEWLINE
            }
            State.READ_NEWLINE -> {
                if (line.isEmpty())
                    return returnState(State.DONE, content)

                return returnState(State.FAILED)
            }
            else -> throw AssertionError()
        }

        return state to null
    }

    enum class State(val value: Int) : Parser.State {
        READY(0),
        READ_NEWLINE(1),
        DONE(100),
        FAILED(200);

        override fun isTerminated() = value >= 100
        override fun isDone() = this == DONE
        override fun isFailed() = value >= 200
    }
}