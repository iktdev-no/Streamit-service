package no.iktdev.streamit.service.db

import no.iktdev.streamit.service.TestBaseWithDatabase
import no.iktdev.streamit.service.db.tables.auth.DelegatedAuthenticationTable
import no.iktdev.streamit.service.db.tables.auth.RegisteredDevicesTable
import no.iktdev.streamit.service.db.tables.content.v2.*
import no.iktdev.streamit.service.db.tables.info.CastErrorTable
import no.iktdev.streamit.service.db.tables.info.DataAudioTable
import no.iktdev.streamit.service.db.tables.info.DataVideoTable
import no.iktdev.streamit.service.db.tables.pfns.PersistentTokenTable
import no.iktdev.streamit.service.db.tables.pfns.TokenTable
import no.iktdev.streamit.service.db.tables.user.ProfileImageTable
import no.iktdev.streamit.service.db.tables.util.withTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.api.Test

class SchemaValidation : TestBaseWithDatabase() {

    @Test
    fun verifySchema() {
        withTransaction {
            val allTables = listOf(
                DelegatedAuthenticationTable,
                RegisteredDevicesTable,
                CatalogTableV2,
                CatalogTitleTableV2,
                CatalogGenreTableV2,
                ContinueWatchTableV2,
                FavoritesTableV2,
                GenreTableV2,
                MovieTableV2,
                ProgressTableV2,
                SerieTableV2,
                SubtitleTableV2,
                SummaryTableV2,
                VideoTableV2,
                CastErrorTable,
                DataAudioTable,
                DataVideoTable,
                PersistentTokenTable,
                TokenTable,
                ProfileImageTable,
                UserTableV2
            )
            SchemaUtils.checkMappingConsistence(*allTables.toTypedArray())
        }
    }
}
