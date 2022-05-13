package kr.ac.kpu.cctvmanager.rsp

import java.net.InetSocketAddress

abstract class Message {
    var remoteAddress: InetSocketAddress? = null

    abstract fun getMessageString(): String
}