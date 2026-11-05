package com.enmanuelbergling.corntime.core.data.remote.dto.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewDto(
    @SerialName("id")
    val id: String,
    @SerialName("author")
    val author: String,
    @SerialName("content")
    val content: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("url")
    val url: String,
    @SerialName("author_details")
    val authorDetails: AuthorDetailsDto,
)

@Serializable
internal data class AuthorDetailsDto(
    @SerialName("name")
    val name: String,
    @SerialName("username")
    val username: String,
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    @SerialName("rating")
    val rating: Float? = null,
)
