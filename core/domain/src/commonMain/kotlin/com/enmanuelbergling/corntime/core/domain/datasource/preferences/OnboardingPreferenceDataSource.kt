package com.enmanuelbergling.corntime.core.domain.datasource.preferences

import kotlinx.coroutines.flow.Flow

interface OnboardingPreferenceDataSource {
    /** True until the user finishes the walkthrough once. */
    fun isOnboarding(): Flow<Boolean>
    suspend fun finishOnboarding()
}
