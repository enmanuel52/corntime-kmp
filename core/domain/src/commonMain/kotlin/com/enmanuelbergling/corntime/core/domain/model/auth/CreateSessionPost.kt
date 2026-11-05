package com.enmanuelbergling.corntime.core.domain.model.auth


data class CreateSessionPost(
    val username: String,
    val password: String,
    val requestToken: String,
)
