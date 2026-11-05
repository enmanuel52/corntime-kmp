package com.enmanuelbergling.corntime.core.domain.design

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
