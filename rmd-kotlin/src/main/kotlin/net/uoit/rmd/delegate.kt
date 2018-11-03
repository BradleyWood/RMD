package net.uoit.rmd

import net.uoit.rmd.delegate.DelegateInfo
import net.uoit.rmd.delegate.FunctionDelegate
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

private val delMap = HashMap<Class<*>, FunctionDelegate<*, *>>()
private val service = Executors.newCachedThreadPool()
private val jobs = Collections.synchronizedList(LinkedList<Future<*>>())

private fun <R> getMethodInfo(func: Function<R>): DelegateInfo {
    return DelegateInfo(func.javaClass, "invoke", "()Ljava/lang/Object;")
}

fun waitForAsyncJobs() {
    while (!jobs.isEmpty()) {
        jobs.removeAt(0).get()
    }
}

fun <R> delegate(func: () -> R): R {
    var delegate = delMap[func.javaClass]

    if (delegate == null) {
        delegate = Rmd.asDelegate(getMethodInfo(func))
    }

    return (delegate as FunctionDelegate<() -> R, R>).invoke(func)
}

fun <R> async(del: () -> R): Future<R> {
    val future = service.submit(Callable {
        delegate(del)
    })

    jobs.add(future)

    return future
}

infix fun <R> Future<R>.callback(callback: (result: R) -> Unit) {
    jobs.add(service.submit {
        callback.invoke(get())
    })
}
