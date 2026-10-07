package io.legado.app.help

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.AppLog
import io.legado.app.utils.FileDoc
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.listFileDocs
import java.io.File

private val fontFileRegex = Regex("(?i).*\\.[ot]tf")

/**
 * 字体扫描结果。
 *
 * @param fontFiles 可用字体：配置的字体文件夹 + 应用私有字体目录，按文件名去重
 * @param folderAccessible 配置的字体文件夹是否读取成功；没有配置文件夹或读取成功时为 true
 */
data class FontScanResult(
    val fontFiles: List<FileDoc>,
    val folderAccessible: Boolean = true,
)

/**
 * 应用私有字体目录：`/Android/data/{package}/files/font`。
 *
 * 通过导入排版配置、主题包，或用应用自带文件管理放进去的字体都在这里，
 * 读取不需要任何 SAF 授权，因此永远可用。
 */
fun privateFontDir(context: Context): File =
    File("${context.getExternalFilesDir(null)?.absolutePath}/font")

fun loadFontFiles(context: Context, folderUri: Uri?): List<FileDoc> =
    scanFontFiles(context, folderUri).fontFiles

/**
 * 扫描字体文件。
 *
 * 两个必须守住的点：
 * 1. 部分国产 ROM 的文件选择器不会给出可持久化的授权（ColorOS / HyperOS 等），
 *    用户选完文件夹后 [Uri] 会被存进设置，但下次读取时抛 SecurityException。
 *    这种情况下不能直接返回空列表，要回落到应用私有字体目录，并把失败原因写进日志，
 *    否则用户看到的就是“没有字体文件”，且永远无法自愈。
 * 2. 应用私有字体目录里的字体（导入的排版/主题包字体）始终参与列表，避免配置了
 *    字体文件夹后就再也看不到应用内字体。
 */
fun scanFontFiles(context: Context, folderUri: Uri?): FontScanResult {
    val privateFonts = privateFontDir(context).listFileDocs { it.name.matches(fontFileRegex) }
    if (folderUri == null) {
        return FontScanResult(privateFonts, true)
    }
    val folderFonts: List<FileDoc>?
    try {
        folderFonts = if (folderUri.isContentScheme()) {
            DocumentFile.fromTreeUri(context, folderUri)
                ?.listFileDocs { it.name.matches(fontFileRegex) }
        } else {
            val file = File(folderUri.path ?: folderUri.toString())
            if (file.exists()) file.listFileDocs { it.name.matches(fontFileRegex) } else null
        }
    } catch (e: Exception) {
        AppLog.put("读取字体文件夹失败, 已回落到应用字体目录: $folderUri", e)
        return FontScanResult(privateFonts, false)
    }
    if (folderFonts == null) {
        AppLog.put("字体文件夹不可访问, 已回落到应用字体目录: $folderUri")
        return FontScanResult(privateFonts, false)
    }
    val merged = LinkedHashMap<String, FileDoc>(folderFonts.size + privateFonts.size)
    folderFonts.forEach { merged[it.name] = it }
    privateFonts.forEach { merged.putIfAbsent(it.name, it) }
    return FontScanResult(merged.values.toList(), true)
}
