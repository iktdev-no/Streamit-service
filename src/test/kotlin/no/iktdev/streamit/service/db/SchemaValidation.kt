package no.iktdev.streamit.service.db

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import no.iktdev.streamit.service.db.tables.auth.RegisteredDevicesTable
import no.iktdev.streamit.service.db.tables.content.CatalogTable
import no.iktdev.streamit.service.db.tables.content.ContinueWatchTable
import no.iktdev.streamit.service.db.tables.content.FavoriteTable
import no.iktdev.streamit.service.db.tables.content.GenreTable
import no.iktdev.streamit.service.db.tables.content.MovieTable
import no.iktdev.streamit.service.db.tables.content.ProgressTable
import no.iktdev.streamit.service.db.tables.content.SerieTable
import no.iktdev.streamit.service.db.tables.content.SubtitleTable
import no.iktdev.streamit.service.db.tables.content.SummaryTable
import no.iktdev.streamit.service.db.tables.content.TitleTable
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