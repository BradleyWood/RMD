package example

import net.uoit.rmd.async
import net.uoit.rmd.callback
import net.uoit.rmd.waitForAsyncJobs

fun main(args: Array<String>) {
    for (i in 0..10) {
        async {
            i + 1
        } callback {
            println("$i + 1 = $it")
        }
    }

    waitForAsyncJobs()
    System.exit(0)
}
