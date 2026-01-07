package com.alexjlockwood.twentyfortyeight.domain

class MutableLimitedList<T>(
    private val base: MutableList<T>,
    private val maxSize: Int,
) : MutableList<T> by base {

    override fun add(element: T): Boolean {
        return if (base.add(element)) {
            while (size > maxSize) {
                removeAt(0)
            }
            true
        } else {
            false
        }
    }
}
