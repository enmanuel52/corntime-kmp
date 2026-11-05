package com.enmanuelbergling.corntime.feature.watchlists.domain.lists

import org.koin.core.annotation.Factory

enum class ListFieldError { NameRequired, DescriptionRequired }

data class ListFormValidation(
    val nameError: ListFieldError? = null,
    val descriptionError: ListFieldError? = null,
) {
    val isValid: Boolean get() = nameError == null && descriptionError == null
}

/** Checks the "create list" form before any request is made. Each platform turns the errors into text. */
@Factory
class ValidateListFormUseCase {
    operator fun invoke(name: String, description: String) = ListFormValidation(
        nameError = ListFieldError.NameRequired.takeIf { name.isBlank() },
        descriptionError = ListFieldError.DescriptionRequired.takeIf { description.isBlank() },
    )
}
