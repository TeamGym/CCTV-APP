package kr.ac.kpu.cctvmanager.rsp.parser

import kr.ac.kpu.cctvmanager.rsp.Message

abstract class Parser<S : Parser.State, T>(protected val initialState: S) {
    var state: S = initialState
        protected set
    var content: T? = null
        protected set

    abstract fun parseLine(line: String): Pair<S, T?>

    open fun reset() {
        state = initialState
        content = null
    }

    protected open fun returnState(state: S, content: T? = null): Pair<S, T?> {
        this.state = state
        return state to content
    }

    interface State {
        fun isTerminated(): Boolean
        fun isDone(): Boolean
        fun isFailed(): Boolean
    }
}