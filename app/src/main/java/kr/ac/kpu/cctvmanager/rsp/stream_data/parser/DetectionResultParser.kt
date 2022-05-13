package kr.ac.kpu.cctvmanager.rsp.stream_data.parser

import kr.ac.kpu.cctvmanager.rsp.parser.Parser
import kr.ac.kpu.cctvmanager.rsp.stream_data.DetectionResult

class DetectionResultParser
    : StreamDataParser<DetectionResultParser.State, DetectionResult>(State.READY) {
    override fun parseLine(line: String): Pair<State, DetectionResult?> {
        when (state) {
            State.READY -> {
                if (line.isEmpty())
                    return returnState(State.FAILED)

                val timestamp = line.toIntOrNull()
                    ?: return returnState(State.FAILED)

                content = DetectionResult(
                    timestamp=timestamp,
                    boxes=mutableListOf())
                state = State.PARSE_BOX
            }
            State.PARSE_BOX -> {
                if (line.isEmpty())
                    return returnState(State.DONE, content)

                val tokens = line.split(",")

                if (tokens.size != 7)
                    return returnState(State.FAILED)

                val left = tokens[0].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val right = tokens[1].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val top = tokens[2].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val bottom = tokens[3].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val confidence = tokens[4].toDoubleOrNull()
                    ?: return returnState(State.FAILED)
                val classID = tokens[5].toIntOrNull()
                    ?: return returnState(State.FAILED)
                val label = tokens[6]

                (content!!.boxes as MutableList).add(
                    DetectionResult.DetectionBox(
                        left=left,
                        right=right,
                        top=top,
                        bottom=bottom,
                        confidence=confidence,
                        classID=classID,
                        label=label
                    ))
            }
            else -> throw AssertionError()
        }

        return state to null
    }

    enum class State(val value: Int) : Parser.State {
        READY(0),
        PARSE_BOX(1),
        DONE(100),
        FAILED(200);

        override fun isTerminated() = value >= 100
        override fun isDone() = this == DONE
        override fun isFailed() = value >= 200
    }
}