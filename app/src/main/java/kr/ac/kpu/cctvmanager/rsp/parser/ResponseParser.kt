package kr.ac.kpu.cctvmanager.rsp.parser

import kr.ac.kpu.cctvmanager.rsp.RSP_VERSION
import kr.ac.kpu.cctvmanager.rsp.Response

class ResponseParser : MessageParser<ResponseParser.State, Response>(State.READY) {
    override fun parseLine(line: String): Pair<State, Response?> {
        when (state) {
            State.READY -> {
                val tokens = line.split(" ")
                if (tokens.size != 2)
                    return returnState(State.FAILED)

                val protocol = tokens[0]

                val (protocolName, protocolVersion) = protocol.split("/", limit=2)

                if (protocolName != "RSP" || protocolVersion != RSP_VERSION)
                    return returnState(State.FAILED_INVALID_PROTOCOL)

                val statusCode = tokens[1].toIntOrNull()
                    ?: return returnState(State.FAILED_INVALID_STATUS)

                content = Response(statusCode)
                state = State.PARSE_SEQUENCE
            }
            State.PARSE_SEQUENCE -> {
                if (!line.startsWith("Seq="))
                    return returnState(State.FAILED)

                val sequence = line.substring(4).toUIntOrNull()
                    ?: return returnState(State.FAILED)

                content!!.sequence = sequence
                state = State.PARSE_PROPERTY
            }
            State.PARSE_PROPERTY -> {
                if (line.isEmpty())
                    return returnState(State.DONE, content)

                if ("=" !in line)
                    return returnState(State.FAILED)

                val (propertyName, propertyValue) = line.split("=", limit=2)

                content!!.addProperty(propertyName, propertyValue)
            }
            else -> throw AssertionError()
        }

        return state to null
    }

    enum class State(val value: Int) : Parser.State {
        READY(0),
        PARSE_SEQUENCE(1),
        PARSE_PROPERTY(2),
        DONE(100),
        FAILED(200),
        FAILED_INVALID_PROTOCOL(201),
        FAILED_INVALID_STATUS(202);

        override fun isTerminated() = value >= 100
        override fun isDone() = this == DONE
        override fun isFailed() = value >= 200
    }

}