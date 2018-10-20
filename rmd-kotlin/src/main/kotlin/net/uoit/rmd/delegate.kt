package net.uoit.rmd

import net.uoit.rmd.delegate.DelegateInfo
import net.uoit.rmd.delegate.FunctionDelegate
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

private val delMap = HashMap<Class<*>, FunctionDelegate<*, *>>()
private val service = Executors.newCachedThreadPool()

private fun <R> getMethodInfo(func: Function<R>): DelegateInfo {
    return DelegateInfo(func.javaClass, "invoke", "()Ljava/lang/Object;")
}

fun <R> delegate(func: () -> R): R {
    var delegate = delMap[func.javaClass]

    if (delegate == null) {
        delegate = Rmd.asDelegate(getMethodInfo(func))
    }

    return (delegate  as FunctionDelegate<() -> R, R>).invoke(func)
}

fun <R> async(del: () -> R): Future<R> {
    return service.submit(Callable {
        delegate(del)
    })
}

infix fun <R> Future<R>.callback(callback: (result: R) -> Unit) {
    service.submit {
        callback.invoke(get())
    }
}
