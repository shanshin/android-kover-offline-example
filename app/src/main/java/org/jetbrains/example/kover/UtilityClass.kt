package org.jetbrains.example.kover

class UtilityClass {
    fun sayHello(postfix: String): String {
        return "Hello $postfix"
    }

    fun unusedFunction() {
        println("unused")
    }
}