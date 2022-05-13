package kr.ac.kpu.cctvmanager.rsp

import java.net.Socket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class SenderThread(val sock: Socket) : Thread() {
    val sendMessageQueue: BlockingQueue<Message?> = LinkedBlockingQueue()
    val sentRequestMap = ConcurrentHashMap<UInt, Request>()
    private var running = true

    fun stopTask() {
        running = false
        sendMessageQueue.clear()
        sendMessageQueue.put(null)
    }

    override fun run() {
        val out = sock.getOutputStream()
        while (running) {
            println("waiting for messages...")
            val message = sendMessageQueue.take()
                ?: break
            println("sending message")
            if (message is Request)
                sentRequestMap[message.sequence] = message
            out.write(message.getMessageString().toByteArray(Charsets.UTF_8))
            out.flush()
        }
    }
}