package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteActionDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxRestClient
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.DEFAULT_PAGE
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.DEFAULT_PAGE_SIZE
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

class WCInboxStoreTest {

    private val restClient: InboxRestClient = mock()
    private val database: WCAndroidDatabase = mock()
    private val inboxNotesDao: InboxNotesDao = mock()

    private val sut = WCInboxStore(
        restClient,
        initCoroutineEngine(),
        database,
        inboxNotesDao
    )

    @Before
    fun setUp() {
        val blockArg1 = argumentCaptor<Runnable>()
        whenever(database.runInTransaction(blockArg1.capture())).then {
            blockArg1.firstValue.run()
        }
    }

    @Test
    fun `Given notes are fetched successfully, when notes fetched, then save them into DB`() =
        test {
            givenNotesAreFetchedSuccesfully()

            sut.fetchInboxNotes(ANY_SITE)

            verify(inboxNotesDao).insertOrUpdateInboxNote(
                ANY_INBOX_NOTE_DTO.toDataModel(ANY_SITE.siteId)
            )
        }

    @Test
    fun `Given notes are fetched successfully, when notes fetched, then return WooResult`() =
        test {
            givenNotesAreFetchedSuccesfully()

            val result = sut.fetchInboxNotes(ANY_SITE)

            Assertions.assertThat(result).isEqualTo(WooResult(Unit))
        }

    @Test
    fun `Given notes fetching error, when notes fetched, then notes are not saved`() =
        test {
            givenErrorFetchingNotes()

            sut.fetchInboxNotes(ANY_SITE)

            verify(inboxNotesDao, never()).insertOrUpdateInboxNote(any())
        }

    @Test
    fun `Given notes fetching error, when notes fetched, then returns WooResult error`() =
        test {
            givenErrorFetchingNotes()

            val result = sut.fetchInboxNotes(ANY_SITE)

            Assertions.assertThat(result).isEqualTo(ANY_WOO_RESULT_WITH_ERROR)
        }

    private suspend fun givenNotesAreFetchedSuccesfully() {
        whenever(
            restClient.fetchInboxNotes(
                ANY_SITE,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(WooPayload(arrayOf(ANY_INBOX_NOTE_DTO)))
    }

    private suspend fun givenErrorFetchingNotes() {
        whenever(
            restClient.fetchInboxNotes(
                ANY_SITE,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(WooPayload(ANY_WOO_API_ERROR))
    }

    private companion object {
        val ANY_WOO_API_ERROR = WooError(GENERIC_ERROR, UNKNOWN)
        val ANY_WOO_RESULT_WITH_ERROR: WooResult<Unit> = WooResult(ANY_WOO_API_ERROR)
        val ANY_INBOX_NOTE_ACTION_DTO = InboxNoteActionDto(
            id = 2,
            name = "action",
            label = "action",
            query = "",
            status = null,
            primary = true,
            actionedText = "",
            nonceAction = "",
            nonceName = "",
            url = "www.automattic.com"
        )
        val ANY_INBOX_NOTE_DTO = InboxNoteDto(
            id = 1,
            name = "",
            type = "",
            status = "",
            isSnoozable = false,
            source = "",
            actions = listOf(ANY_INBOX_NOTE_ACTION_DTO),
            locale = "",
            title = "",
            content = "",
            layout = "",
            dateCreated = "",
            dateReminder = ""
        )
        val ANY_SITE = SiteModel().apply { siteId = 1 }
    }
}