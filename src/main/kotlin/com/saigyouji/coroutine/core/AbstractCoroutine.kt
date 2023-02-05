package com.saigyouji.coroutine.core

import com.saigyouji.coroutine.CancellationException
import com.saigyouji.coroutine.Job
import com.saigyouji.coroutine.scope.CoroutineScope
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

abstract class AbstractCoroutine<T>(context: CoroutineContext) : Job, Continuation<T>, CoroutineScope{
    protected val state = AtomicReference<CoroutineState>()
    override val context: CoroutineContext

    override val scopeContext: CoroutineContext
        get() = context
    protected val parentJob = context[Job]

    private var parentCancelDisposable: Disposable? = null

    init{
        state.set(CoroutineState.InComplete())
        this.context = context + this

        parentCancelDisposable = parentJob?.attachChild(this)
    }

    val isCompleted
        get() = state.get() is CoroutineState.Complete<*>

    override val isActive: Boolean
        get() = when (val currentState = state.get()){
            is CoroutineState.Complete<*>,
            is CoroutineState.Cancelling -> false
            is CoroutineState.CompleteWaitForChildren<*> -> ! currentState.isCancelling
            is CoroutineState.InComplete -> true
        }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet{ prevState ->
            when(prevState){
                is CoroutineState.Cancelling,
                is CoroutineState.InComplete -> prevState.tryComplete(result)
                is CoroutineState.CompleteWaitForChildren<*> ,
                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("Already completed!")
                }
            }
        }

        when(newState){
            is CoroutineState.CompleteWaitForChildren<*> -> newState.tryWaitForChildren(::tryCompleteOnChildCompleted)
            is CoroutineState.Complete<*> -> makeCompletion(newState as CoroutineState.Complete<T>)
            else -> {}
        }
    }
    private fun tryCompleteOnChildCompleted(child: Job){
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.Cancelling,
                is CoroutineState.InComplete -> {
                    throw IllegalStateException("Should be waiting for children!")
                }
                is CoroutineState.CompleteWaitForChildren<*> -> {
                    prev.onChildCompleted(child)
                }
                is CoroutineState.Complete<*> -> throw IllegalStateException("Already completed!")
            }
        }

        (newState as? CoroutineState.Complete<T>)?.let {
            makeCompletion(it)
        }
    }

    private fun makeCompletion(newState: CoroutineState.Complete<T>){
        val result = if (newState.exception == null) {
            Result.success(newState.value)
        }else{
            Result.failure(newState.exception)
        }
        result.exceptionOrNull()?.let(this::tryHandleException)

        newState.notifyCompletion(result)
        newState.clear()
        parentCancelDisposable?.dispose()
    }

    override suspend fun join() {
       when(state.get()){
           is CoroutineState.InComplete,
           is CoroutineState.CompleteWaitForChildren<*>,
           is CoroutineState.Cancelling -> return joinSuspend()
           is CoroutineState.Complete<*> -> {
               val currentCallingJobState = coroutineContext[Job]?.isActive ?: return
               if(!currentCallingJobState){
                   throw CancellationException("Coroutine is cancelled.")
               }
               return
           }
       }
    }
    private suspend fun joinSuspend(){

    }
    private fun tryHandleException(exception: Throwable){

    }
}