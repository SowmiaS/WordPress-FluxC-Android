package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions

@Dao
abstract class InboxNotesDao {
    @Transaction
    @Query("SELECT * FROM InboxNotes WHERE siteId = :siteId ORDER BY dateCreated DESC")
    abstract fun observeInboxNotes(siteId: Long): Flow<List<InboxNoteWithActions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNote(entity: InboxNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateInboxNoteAction(entity: InboxNoteActionEntity)

    @Query("DELETE FROM InboxNotes WHERE siteId = :siteId")
    abstract suspend fun deleteSiteInboxNotes(siteId: Long)
}
