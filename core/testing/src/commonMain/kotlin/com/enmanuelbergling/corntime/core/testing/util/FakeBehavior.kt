package com.enmanuelbergling.corntime.core.testing.util

import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import kotlinx.coroutines.CompletableDeferred

/**
 * Programmable behaviour shared by the fake data sources:
 * - [fail] makes a function return a typed error until [recover]ed,
 * - [hold] suspends every call until [release] (to assert loading states / concurrent calls),
 * - [callsTo] exposes the arguments each function received, in order.
 */
abstract class FakeBehavior<F : Enum<F>> {
    private val failures = mutableMapOf<F, DataError.Network>()
    private val calls = mutableListOf<Pair<F, List<Any?>>>()
    private var gate: CompletableDeferred<Unit>? = null

    fun fail(function: F, error: DataError.Network = DataError.Network.Unknown) {
        failures[function] = error
    }

    fun failAll(functions: Array<F>, error: DataError.Network = DataError.Network.Unknown) =
        functions.forEach { fail(it, error) }

    fun recover(function: F) {
        failures.remove(function)
    }

    fun recoverAll() = failures.clear()

    fun hold() {
        gate = CompletableDeferred()
    }

    fun release() {
        gate?.complete(Unit)
        gate = null
    }

    fun callsTo(function: F): List<List<Any?>> = calls.filter { it.first == function }.map { it.second }

    fun callCount(function: F): Int = callsTo(function).size

    protected suspend fun <T> respond(function: F, vararg args: Any?, success: () -> T): NetworkResult<T> {
        calls += function to args.toList()
        gate?.await()
        return failures[function]?.let { Result.Failure(it) } ?: Result.Success(success())
    }
}
