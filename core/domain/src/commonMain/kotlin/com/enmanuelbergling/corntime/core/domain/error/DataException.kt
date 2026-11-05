package com.enmanuelbergling.corntime.core.domain.error

/**
 * Carries a typed [Error] through a [com.enmanuelbergling.corntime.core.domain.design.ChainHandler],
 * whose handlers signal failure by throwing (same contract as the iOS design).
 */
class DataException(val error: Error) : Exception(error.toString())
