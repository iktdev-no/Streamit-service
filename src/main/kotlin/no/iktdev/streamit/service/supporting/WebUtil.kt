package no.iktdev.streamit.service.supporting

import java.io.File
import java.nio.file.Path

class WebUtil {
    fun renderDirectoryListing(
        baseUrl: String,
        pathPrefix: String,
        currentPath: Path,
        entries: List<Pair<String, Boolean>> // Pair(name, isDirectory)
    ): String {
        val parent = currentPath.parent?.let {
            val up = it.toString().replace(File.separator, "/").removePrefix(pathPrefix)
            """<li><a href="$baseUrl${if (up.isBlank()) "/" else up}">.. (up)</a></li>"""
        } ?: ""

        val items = entries.joinToString("\n") { (name, isDir) ->
            val slash = if (isDir) "/" else ""
            """<li><a href="$baseUrl${(currentPath.resolve(name)).toString().removePrefix(pathPrefix).replace(File.separator, "/")}$slash">$name$slash</a></li>"""
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Directory Listing</title>
            <style>
                body {
                    background: #121212;
                    color: #e0e0e0;
                    font-family: monospace;
                    padding: 1rem;
                }
                h2 { color: #90caf9; }
                a {
                    color: #81d4fa;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                ul {
                    list-style: none;
                    padding-left: 0;
                }
            </style>
        </head>
        <body>
            <h2>📁 Browsing: /${currentPath.toString().removePrefix(pathPrefix)}</h2>
            <ul>
                $parent
                $items
            </ul>
        </body>
        </html>
    """.trimIndent()
    }

}