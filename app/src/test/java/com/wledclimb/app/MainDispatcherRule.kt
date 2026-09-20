package com.wledclimb.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a test dispatcher. Anything using
 * `viewModelScope` needs this - without it a local JVM test fails with
 * "Module with the Main dispatcher had failed to initialize".
 *
 * Defaults to [UnconfinedTestDispatcher] so coroutines launched from a
 * ViewModel's `init` have already run by the time the constructor returns,
 * which keeps tests free of manual scheduler advancing.
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
