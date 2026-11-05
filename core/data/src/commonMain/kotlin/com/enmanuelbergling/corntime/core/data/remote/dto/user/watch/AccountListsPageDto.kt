package com.enmanuelbergling.corntime.core.data.remote.dto.user.watch


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountListsPageDto(
    @SerialName("page")
    val page: Int,
    @SerialName("results")
    val results: List<WatchListDto>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int,
)