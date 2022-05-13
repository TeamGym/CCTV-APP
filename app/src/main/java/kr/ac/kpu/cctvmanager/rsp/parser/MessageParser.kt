package kr.ac.kpu.cctvmanager.rsp.parser

import kr.ac.kpu.cctvmanager.rsp.Message

abstract class MessageParser<S : Parser.State, T : Message>(state: S) : Parser<S, T>(state)