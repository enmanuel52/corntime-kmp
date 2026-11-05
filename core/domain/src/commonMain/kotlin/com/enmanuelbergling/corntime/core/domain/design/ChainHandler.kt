package com.enmanuelbergling.corntime.core.domain.design

import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.error.Error
import com.enmanuelbergling.corntime.core.domain.util.EmptyResult
import com.enmanuelbergling.corntime.core.domain.util.Result

/**
 * Chain of responsibility, ported from the CornTime iOS design.
 *
 * Every handler receives the same mutable [Request], fills the part it is responsible for and
 * passes it along to [next]. A handler signals failure by throwing; that stops the chain, so
 * later handlers never run on a partially failed request.
 */
interface ChainHandler<Request> {
    val next: ChainHandler<Request>?

    suspend fun handle(request: Request)
}

/** Runs this handler and then the rest of the chain, in order. */
suspend fun <Request> ChainHandler<Request>.invoke(request: Request) {
    handle(request)
    next?.invoke(request)
}

/**
 * Runs the chain and reports a [DataException] as a typed failure instead of throwing, so a ViewModel
 * can keep whatever the handlers filled before the failing one. Cancellation and unexpected
 * exceptions still propagate.
 */
suspend fun <Request> ChainHandler<Request>.invokeForResult(request: Request): EmptyResult<Error> =
    try {
        invoke(request)
        Result.Success(Unit)
    } catch (exception: DataException) {
        Result.Failure(exception.error)
    }
