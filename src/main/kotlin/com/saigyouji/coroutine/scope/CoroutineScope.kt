package com.saigyouji.coroutine.scope

import com.saigyouji.coroutine.Job
import kotlin.coroutines.CoroutineContext

interface CoroutineScope {
    val scopeContext: CoroutineContext
}
internal class ContextScope(context: CoroutineContext): CoroutineScope{
    override val scopeContext: CoroutineContext = context
}

operator fun CoroutineScope.plus(context: CoroutineContext): CoroutineScope =
    ContextScope(scopeContext + context)

fun CoroutineScope.cancel(){
    val job = scopeContext[Job]

}

