package com.saigyouji.coroutine.core

import com.saigyouji.coroutine.Job
import com.saigyouji.coroutine.OnCancel

typealias OnCompleteT<T> = (Result<T>) -> Unit
interface Disposable {
    fun dispose()
}

class CompletionHandlerDisposable<T>(val job: Job, val onComplete: OnCompleteT<T>) : Disposable{
    override fun dispose() {
       job.remove(this)
    }
}
class CancellationHandlerDisposable(val job: Job, val onCancel: OnCancel): Disposable{
    override fun dispose() {
       job.remove(this)
    }
}