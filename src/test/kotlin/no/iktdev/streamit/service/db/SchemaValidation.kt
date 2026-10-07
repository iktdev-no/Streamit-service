package no.iktdev.streamit.service.db

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import no.iktdev.streamit.service.db.tables.auth.RegisteredDevicesTable
import no.iktdev.streamit.service.db.tables.info.CastErrorTable
import no.iktdev.streamit.service.db.tables.info.DataAudioTable
import no.iktdev.streamit.service.db.tables.info.DataVideoTable
import no.iktdev.streamit.service.db.tables.pfns.PersistentTokenTable
import no.iktdev.streamit.service.db.tables.pfns.TokenTable
import no.iktdev.streamit.service.db.tables.user.ProfileImageTable
import no.iktdev.streamit.service.db.tables.user.UserTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.api.Test

class SchemaValidation: TestBaseWithDatabase() {

    @Test
    fun verifySchema() {
        withTransaction {
            val allTables = listOf(
                DelegatedAuthenticationTable,
                RegisteredDevicesTable,
                CatalogTable,
                ContinueWatchTable,
                FavoriteTable,
                GenreTable,
                MovieTable,
                ProgressTable,
                SerieTable,
                SubtitleTable,
                SummaryTable,
                TitleTable,
                CastErrorTable,
                DataAudioTable,
                DataVideoTable,
                PersistentTokenTable,
                TokenTable,
                ProfileImageTable,
                UserTable
            )
            SchemaUtils.checkMappingConsistence(*allTables.toTypedArray())
        }
    }


}