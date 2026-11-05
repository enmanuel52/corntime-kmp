package com.enmanuelbergling.corntime.core.data.remote.mapper

import com.enmanuelbergling.corntime.core.data.remote.dto.user.UserDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.CreateListBody
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.WatchListDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.WatchResponseDto
import com.enmanuelbergling.corntime.core.domain.model.user.CreateListPost
import com.enmanuelbergling.corntime.core.domain.model.user.WatchList
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse

internal fun UserDetailsDto.toModel() = UserDetails(
    id = id, username = username, avatarPath = avatar.tmdb.avatarPath.orEmpty(), name = name
)

internal fun CreateListPost.asBody() = CreateListBody(name, description, language)

internal fun WatchResponseDto.toModel() = WatchResponse(statusMessage)

internal fun WatchListDto.toModel() = WatchList(
    description = description,
    favoriteCount = favoriteCount,
    id = id,
    itemCount = itemCount,
    iso6391 = iso6391,
    listType = listType,
    name = name,
    posterPath = posterPath
)