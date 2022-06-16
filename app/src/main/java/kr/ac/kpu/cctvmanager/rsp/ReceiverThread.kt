package kr.ac.kpu.cctvmanager.rsp

import kr.ac.kpu.cctvmanager.rsp.parser.MessageParser
import kr.ac.kpu.cctvmanager.rsp.parser.RequestParser
import kr.ac.kpu.cctvmanager.rsp.parser.ResponseParser
import kr.ac.kpu.cctvmanager.rsp.parser.StreamParser
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class ReceiverThread(val sock: Socket) : Thread() {
    val receiveMessageQueue: BlockingQueue<Message?> = LinkedBlockingQueue()

    private var running = true
    private val parserList = mutableListOf(
        StreamParser(),
        RequestParser(),
        ResponseParser()
    )
    private var parserIndex = 0

    fun stopTask() {
        running = false
    }

    override fun run() {
        val reader = sock.getInputStream().bufferedReader(Charsets.UTF_8)

        while (running) {
            val line = reader.readLine()
            for (i in 0..parserList.size) {
                val parser = parserList[(parserIndex + i) % parserList.size]

                val (state, message) = parser.parseLine(line)

                if (state.isDone()) {
                    println("received message: \n\n${message!!.getMessageString()}")
                    message.remoteAddress = sock.remoteSocketAddress as InetSocketAddress
                    receiveMessageQueue.put(message)
                    parser.reset()
                }

                if (!state.isFailed()) {
                    Collections.rotate(parserList, -1)
                    break
                }

                parser.reset()
            }
        }
    }
}