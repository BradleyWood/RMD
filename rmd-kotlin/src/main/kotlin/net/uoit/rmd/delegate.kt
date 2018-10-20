package net.uoit.rmd

import net.uoit.rmd.delegate.DelegateInfo
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

private var service = Executors.newCachedThreadPool()

private fun <R> getMethodInfo(func: Function<R>): DelegateInfo {
    return DelegateInfo(func.javaClass, "invoke", "()Ljava/lang/Object;")
}

fun <R> delegate(del: () -> R): R {
    return Rmd.asDelegate(getMethodInfo(del)).invoke(del) as R
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
