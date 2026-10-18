package com.lin0721.linmusic.core.ui.theme.vibrant

// and  node-vibrant  of  PQueue ：push "Default Order"，
// pop/toList ，pop  of （）
internal class VibrantPQueue<T>(private val comparator: Comparator<T>) {
    private val contents = ArrayList<T>()
    private var sorted = false

    private fun ensureSorted() {
        if (!sorted) {
            contents.sortWith(comparator)
            sorted = true
        }
    }

    fun push(item: T) {
        contents.add(item)
        sorted = false
    }

    fun pop(): T? {
        ensureSorted()
        return if (contents.isEmpty()) null else contents.removeAt(contents.size - 1)
    }

    fun size(): Int = contents.size

    fun toList(): List<T> {
        ensureSorted()
        return contents.toList()
    }
}
