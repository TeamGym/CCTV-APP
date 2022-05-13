package kr.ac.kpu.cctvmanager.rsp

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class RspConnection(val endpointType: EndpointType,
                    val sock: Socket,
                    requestHandlers: Map<Request.Method, RequestHandler> = mapOf(),
                    streamHandlers: Map<Int, StreamHandler> = mapOf(),
                    eventHandlers: Map<Class<out Event>, MutableList<EventHandler<out Event>>> = mapOf()) {
    private val requestHandlers = ConcurrentHashMap(requestHandlers)
    private val streamHandlers = ConcurrentHashMap(streamHandlers)
    private val eventHandlers = ConcurrentHashMap(eventHandlers)

    private val senderThread = SenderThread(sock)
    private val receiverThread = ReceiverThread(sock)
    private val messageEventThread = MessageEventThread(
        receiveMessageQueue=receiverThread.receiveMessageQueue,
        sentRequestMap=senderThread.sentRequestMap,
        onRequestReceived=::onRequest,
        onStreamReceived=::onStream)

    private var sequenceCount: UInt = 0U

    fun sendRequest(request: Request) {
        request.sequence = sequenceCount
        sequenceCount += 1U // overflow intended

        senderThread.sendMessageQueue.put(request)
    }

    fun sendResponse(response: Response, requestReceived: Request) {
        assert(!requestReceived.delivered)

        requestReceived.delivered = true
        response.sequence = requestReceived.sequence

        senderThread.sendMessageQueue.put(response)
    }

    fun sendStream(stream: Stream) {
        senderThread.sendMessageQueue.put(stream)
    }

    fun addRequestHandler(method: Request.Method, requestHandler: RequestHandler) {
        requestHandlers[method] = requestHandler
    }

    fun addStreamHandler(channel: Int, streamHandler: StreamHandler) {
        streamHandlers[channel] = streamHandler
    }

    fun addEventHandler(eventClass: Class<out Event>, eventHandler: EventHandler<out Event>) {
        if (eventClass !in eventHandlers.keys)
            eventHandlers[eventClass] = mutableListOf()
        eventHandlers[eventClass]!!.add(eventHandler)
    }

    private fun fireEvent(event: Event) {
        for (handler in eventHandlers[event::class.java]!!) {
            handler::class.java.getDeclaredMethod("onEvent").invoke(handler, event)
        }
    }

    private fun onRequest(request: Request) {
        requestHandlers[request.method]?.onRequest(request) { response ->
            sendResponse(response, request)
        }
    }

    private fun onStream(stream: Stream) {
        streamHandlers[stream.channel]?.onStream(stream)
    }

    private fun onException(throwable: Throwable) {
        fireEvent(ExceptionEvent(throwable))
    }

    fun start() {
        senderThread.isDaemon = true
        receiverThread.isDaemon = true
        messageEventThread.isDaemon = true

        senderThread.setUncaughtExceptionHandler { _, throwable -> onException(throwable) }
        receiverThread.setUncaughtExceptionHandler { _, throwable -> onException(throwable) }
        messageEventThread.setUncaughtExceptionHandler { _, throwable -> onException(throwable) }

        senderThread.start()
        receiverThread.start()
        messageEventThread.start()

        println("Start RspConnection")
    }

    companion object {
        @JvmStatic
        fun makeConnection(endpointType: EndpointType,
                           remote: InetSocketAddress,
                           requestHandlers: Map<Request.Method, RequestHandler> = mapOf(),
                           streamHandlers: Map<Int, StreamHandler> = mapOf(),
                           eventHandlers: Map<Class<out Event>, MutableList<EventHandler<out Event>>> = mapOf())
                : RspConnection {
            val sock = Socket()
            sock.connect(remote)

            val conn = RspConnection(
                endpointType,
                sock,
                requestHandlers,
                streamHandlers,
                eventHandlers)
            conn.start()
            return conn
        }
    }

    interface Event
    data class ExceptionEvent(val throwable: Throwable) : Event

    fun interface EventHandler<T: Event> {
        fun onEvent(event: T)
    }

    interface RequestHandler {
        fun onRequest(request: Request, returnResponse: (Response) -> Unit)
    }
    interface StreamHandler {
        fun onStream(stream: Stream)
    }
}