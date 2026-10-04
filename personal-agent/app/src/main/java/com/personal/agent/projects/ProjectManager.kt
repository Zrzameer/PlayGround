package com.personal.agent.projects

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ProjectInfo(val id: String, val name: String, val path: String, val fileCount: Int)

class ProjectManager(private val context: Context) {
    val root: File get() = File(context.filesDir, "workspaces").apply { mkdirs() }

    suspend fun list(): List<ProjectInfo> = withContext(Dispatchers.IO) {
        root.listFiles()?.filter { it.isDirectory }?.map { dir ->
            val count = dir.walkTopDown().count { it.isFile }.coerceAtMost(100000)
            ProjectInfo(dir.name, dir.name, dir.absolutePath, count)
        } ?: emptyList()
    }

    suspend fun create(name: String): ProjectInfo = withContext(Dispatchers.IO) {
        val safe = name.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").ifEmpty { "project" }
        val dir = File(root, "${safe}_${UUID.randomUUID().toString().take(6)}")
        dir.mkdirs()
        File(dir, "README.md").writeText("# $name\n\nCreated with Personal Agent (Phase 1).\n")
        File(dir, ".agent-context.md").writeText("# Agent context\n\n- Project: $name\n")
        ProjectInfo(dir.name, name, dir.absolutePath, 2)
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        File(root, id).deleteRecursively()
    }

    suspend fun importZip(zipFile: File, name: String): ProjectInfo = withContext(Dispatchers.IO) {
        val info = create(name)
        // Safe unzip: block Zip-Slip paths.
        java.util.zip.ZipFile(zipFile).use { zip ->
            val dest = File(info.path).canonicalFile
            zip.entries().asSequence().forEach { e ->
                val out = File(dest, e.name).canonicalFile
                if (!out.path.startsWith(dest.path)) return@forEach
                if (e.isDirectory) out.mkdirs()
                else {
                    out.parentFile?.mkdirs()
                    zip.getInputStream(e).use { ins -> out.outputStream().use { ins.copyTo(it) } }
                }
            }
        }
        list().first { it.id == info.id }
    }
}
