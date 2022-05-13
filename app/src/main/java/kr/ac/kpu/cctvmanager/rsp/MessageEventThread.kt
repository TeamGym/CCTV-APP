package kr.ac.kpu.cctvmanager.rsp

import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap

class MessageEventThread(private val receiveMessageQueue: BlockingQueue<Message?>,
                         private val sentRequestMap: ConcurrentHashMap<UInt, Request>,
                         private val onRequestReceived: (Request) -> Unit,
                         private val onStreamReceived: (Stream) -> Unit) : Thread() {
    private var running = true

    fun stopTask() {
        running = false
        receiveMessageQueue.clear()
        receiveMessageQueue.put(null)
    }

    override fun run() {
        while (running) {
            val message = receiveMessageQueue.take()
                ?: break

            when (message) {
                is Stream -> onStreamReceived(message)
                is Request -> onRequestReceived(message)
                is Response -> {
                    if (message.sequence !in sentRequestMap.keys) {
                        println("no matching result for response (sequence=${message.sequence}")
                        return
                    }

                    sentRequestMap[message.sequence]!!.responseCallback!!(message)
                    sentRequestMap.remove(message.sequence)
                }
            }
        }
    }
}