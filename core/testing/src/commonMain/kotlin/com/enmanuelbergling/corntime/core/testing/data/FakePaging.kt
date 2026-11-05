package com.enmanuelbergling.corntime.core.testing.data

import com.enmanuelbergling.corntime.core.domain.model.core.PageModel

/**
 * Builds page [page] out of [totalPages] from [items], giving every element a unique id per page
 * through [withId] so pages can be told apart and list keys stay unique.
 */
fun <T> fakePage(
    items: List<T>,
    page: Int,
    totalPages: Int,
    withId: T.(Int) -> T,
): PageModel<T> {
    if (page > totalPages || totalPages == 0) return PageModel(emptyList(), totalPages)
    return PageModel(
        results = items.mapIndexed { index, item -> item.withId(page * 1_000 + index) },
        totalPages = totalPages,
    )
}
