package com.enmanuelbergling.corntime.core.domain.datasource.preferences

import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import kotlinx.coroutines.flow.Flow

interface UserPreferenceDataSource {
    /** Emits null while no user is logged in. */
    fun getCurrentUser(): Flow<UserDetails?>
    suspend fun updateUser(userDetails: UserDetails)
    suspend fun clear()
}
