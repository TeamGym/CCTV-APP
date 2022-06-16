package kr.ac.kpu.cctvmanager.rsp.stream_data.parser

import kr.ac.kpu.cctvmanager.rsp.parser.Parser
import kr.ac.kpu.cctvmanager.rsp.stream_data.StreamData

abstract class StreamDataParser<S : Parser.State, T : StreamData>(state: S) : Parser<S, T>(state)
