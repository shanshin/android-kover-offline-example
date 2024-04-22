package org.jetbrains.example.kover

class MyClass {
    fun doSomeAction(): String {
        val result = UtilityClass().sayHello("World!")
        return "result: $result"
    }
}