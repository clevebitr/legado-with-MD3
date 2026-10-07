package io.legado.app.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 字体文件夹的两种来源：
 * - 系统选择器：SAF content:// tree URI；
 * - 自带文件夹选择器（系统选择器列不出字体时的兜底）：file:// 路径。
 *
 * 这里只测不依赖 Android 运行时的文件目录扫描，即兜底路径的核心：
 * 必须和 SAF 一样按 `.ttf/.otf`（不分大小写）过滤。
 */
class FontLoaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun listFontFilesInDir_keepsTtfAndOtf_ignoresOthers() {
        val dir = tempFolder.newFolder("fonts").apply {
            File(this, "regular.ttf").writeBytes(byteArrayOf(0, 1))
            File(this, "serif.OTF").writeBytes(byteArrayOf(0, 1))
            File(this, "readme.txt").writeBytes(byteArrayOf(0, 1))
        }

        val names = listFontFilesInDir(dir)!!.map { it.name }.sorted()

        assertEquals(listOf("regular.ttf", "serif.OTF"), names)
    }

    @Test
    fun listFontFilesInDir_returnsNullWhenPathIsNotDirectory() {
        val missing = File(tempFolder.root, "font_not_exists")

        assertNull("不存在的路径必须返回 null，宿主据此提示不可访问", listFontFilesInDir(missing))
    }
}
