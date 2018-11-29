package example

import net.uoit.rmd.async
import net.uoit.rmd.callback
import net.uoit.rmd.waitForAsyncJobs
import java.util.*
import kotlin.collections.ArrayList


fun main(args: Array<String>) {
    val numbers = 10.randomList(0, 100)
    println("List of Numbers: $numbers")

    sortJob(numbers)

    waitForAsyncJobs()
    System.exit(0)
}

fun sortJob(list: MutableList<Int>) = async {
    list.sort()
    list
} callback {
    println("Sort Result: $it")
}

fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) + start

fun Int.randomList(min: Int, max: Int): MutableList<Int> {
    val numbers: ArrayList<Int> = ArrayList()

    for (i in 0..this) {
        numbers.add((min..max).random())
    }

    return numbers
}
