package net.uoit.rmd

import net.uoit.rmd.delegate.DelegateInfo

private fun <R> getMethodInfo(func: Function<R>): DelegateInfo {
    return DelegateInfo(func.javaClass, "invoke", "()Ljava/lang/Object;")
}

fun <R> delegate(del: () -> R): R {
    return Rmd.asDelegate(getMethodInfo(del)).invoke(del) as R
}
