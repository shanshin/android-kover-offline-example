package org.jetbrains.example.kover

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.kover.offline.runtime.api.KoverRuntime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Base64

/**
 * Kover Instrumented test
 */
@RunWith(AndroidJUnit4::class)
class KoverTest {
    @Test
    fun useAppContext() {
        MyClass().doSomeAction()

        // get Kover binary report
        val reportDump = KoverRuntime.getReport()

        // save Kover binary report
        println("KOVER DUMP=${Base64.getEncoder().encodeToString(reportDump)}")
    }
}