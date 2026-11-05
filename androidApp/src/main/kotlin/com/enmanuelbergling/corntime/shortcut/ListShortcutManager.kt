package com.enmanuelbergling.corntime.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.enmanuelbergling.corntime.CornTimeActivity
import com.enmanuelbergling.corntime.R
import com.enmanuelbergling.corntime.feature.watchlists.presentation.details.ListShortcuts

/** A list the user opened through a launcher shortcut. */
data class ListTarget(val listId: Int, val listName: String)

/** Launcher shortcuts for the lists the user created: one tap opens the list. */
class ListShortcutManager(private val context: Context) : ListShortcuts {

    override fun isPinned(listId: Int): Boolean =
        ShortcutManagerCompat.getDynamicShortcuts(context).any { it.id == shortcutId(listId) }

    override fun add(listId: Int, listName: String) {
        val intent = Intent(context, CornTimeActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_LIST_ID, listId)
            putExtra(EXTRA_LIST_NAME, listName)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId(listId))
            .setShortLabel(listName)
            .setLongLabel(listName)
            .setIcon(IconCompat.createWithResource(context, R.drawable.bookmark_icon))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    override fun remove(listId: Int) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId(listId)))
    }

    /** Called on logout: the shortcuts belong to the account. */
    fun removeAll() = ShortcutManagerCompat.removeAllDynamicShortcuts(context)

    companion object {
        const val EXTRA_LIST_ID = "watch_list_id_extra"
        const val EXTRA_LIST_NAME = "watch_list_name_extra"
        private const val NO_LIST = -1

        fun shortcutId(listId: Int) = "watchlist-$listId"

        /** The list a launch intent asks to open, or null for a normal launch. */
        fun targetOf(intent: Intent?): ListTarget? {
            val listId = intent?.getIntExtra(EXTRA_LIST_ID, NO_LIST) ?: return null
            if (listId == NO_LIST) return null
            return ListTarget(listId, intent.getStringExtra(EXTRA_LIST_NAME).orEmpty())
        }
    }
}
