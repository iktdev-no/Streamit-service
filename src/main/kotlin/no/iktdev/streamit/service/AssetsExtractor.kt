package no.iktdev.streamit.service

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.io.File

@Component
class AssetExtractor {

    @EventListener(ApplicationReadyEvent::class)
    fun unpackAssetsOnStartup() {
        val outputDir = Env.getAssetsFolder().also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        if (outputDir.with(".init").exists()) {
            return
        }

        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath:/assets/*")

        for (resource in resources) {
            val targetFile = File(outputDir, resource.filename ?: continue)
            resource.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        println("✅ Assets pakket ut ved oppstart. ${outputDir.absolutePath}")
    }
}