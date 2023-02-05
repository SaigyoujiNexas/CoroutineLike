package com.saigyouji.coroutine.cancel

import kotlin.coroutines.Continuation
import kotlin.jvm.internal.Intrinsics
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.intercepted
class CancellableContinuation<T>(private val continuation: Continuation<T>): Continuation<T> by continuation {
    fun resume(value: T){}
    fun invokeOnCancellation(v : () -> Unit){
        TODO("not implemented")
    }
    fun getResult(): Any?{
        TODO("not implemented")
        return null
    }
}

suspend inline fun <reified T> suspendCancellableCoroutine(
    crossinline block: (CancellableContinuation<T>) -> Unit
): T = suspendCoroutineUninterceptedOrReturn{continuation ->
    val cancellable = CancellableContinuation(continuation.intercepted())
    block(cancellable)
    cancellable.getResult()

}