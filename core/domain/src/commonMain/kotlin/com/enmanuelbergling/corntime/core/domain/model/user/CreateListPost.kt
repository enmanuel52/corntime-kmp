package com.enmanuelbergling.corntime.core.domain.model.user


data class CreateListPost(
    val name: String,
    val description: String,
    val language: String = "en",
)
