package io.legado.app.ui.file

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.utils.toastOnUi
import java.io.File

class FileManageViewModel(application: Application) : BaseViewModel(application) {

    var rootDoc: File? = context.getExternalFilesDir(null)?.parentFile

    var subDocs = mutableListOf<File>()
    val filesLiveData = MutableLiveData<List<File>>()

    val lastDir: File? get() = subDocs.lastOrNull() ?: rootDoc

    /**
     * 切换浏览根目录（如内置字体文件夹选择器把根设在共享存储）。
     * 必须在首次 [upFiles] 之前调用，否则会带着旧路径的 [subDocs]。
     */
    fun setRootDir(root: File?) {
        rootDoc = root
        subDocs.clear()
    }

    fun upFiles(parentFile: File?) {
        execute {
            parentFile ?: return@execute emptyList()
            if (parentFile == rootDoc) {
                parentFile.listFiles()?.sortedWith(
                    compareBy({ it.isFile }, { it.name })
                )
            } else {
                val list = arrayListOf(parentFile)
                parentFile.listFiles()?.sortedWith(
                    compareBy({ it.isFile }, { it.name })
                )?.let {
                    list.addAll(it)
                }
                list
            }
        }.onStart {
            filesLiveData.postValue(emptyList())
        }.onSuccess {
            filesLiveData.postValue(it ?: emptyList())
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

    fun delFile(file: File) {
        execute {
            file.delete()
        }.onSuccess {
            upFiles(lastDir)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}