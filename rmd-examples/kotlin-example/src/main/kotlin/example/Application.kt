package example

import net.uoit.rmd.async
import net.uoit.rmd.callback
import java.lang.Thread.sleep

fun main(args: Array<String>) {
    for (i in 0..10) {
        async {
            i + 1
        } callback {
            println("i = $it")
        }
    }

    sleep(1000)

    System.exit(0)
}
