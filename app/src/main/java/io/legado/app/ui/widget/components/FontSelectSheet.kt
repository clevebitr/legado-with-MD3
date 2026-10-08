package io.legado.app.ui.widget.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.R
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.ui.file.FileManageActivity
import io.legado.app.ui.theme.ProvideAppDensity
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenu
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenuItem
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.utils.FileDoc
import io.legado.app.utils.isContentScheme
import java.io.File

@Composable
fun FontSelectSheet(
    show: Boolean = true,
    title: String,
    folderState: FontFolderState,
    selectedFontPath: String?,
    onDismissRequest: () -> Unit,
    onSelectFont: (FileDoc) -> Unit,
    onOpenFolderPicker: () -> Unit,
    /** 非空时文件夹按钮改为“系统/自带”二选一，并把内置选择器结果回调给宿主。 */
    onSelectFontFolder: ((Uri) -> Unit)? = null,
    startAction: (@Composable () -> Unit)? = null,
    folderIcon: ImageVector = Icons.Default.FolderOpen,
    folderContentDescription: String? = null,
    onSelectSystemTypeface: ((Int) -> Unit)? = null,
    systemTypefaces: Array<String>? = null,
    emptyText: String? = null,
) {
    val context = LocalContext.current
    val selectedFontName = remember(selectedFontPath) {
        selectedFontPath?.let {
            runCatching {
                val uri = it.toUri()
                if (uri.isContentScheme()) {
                    DocumentFile.fromSingleUri(context, uri)?.name
                } else {
                    File(uri.path ?: it).name
                }
            }.getOrNull()
        }
    }
    var showTypefaceMenu by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    val appFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(FileManageActivity.EXTRA_RESULT_FOLDER)?.let { path ->
                onSelectFontFolder?.invoke(Uri.fromFile(File(path)))
            }
        }
    }
    val launchAppFolderPicker: () -> Unit = {
        val intent = Intent(context, FileManageActivity::class.java).apply {
            putExtra(FileManageActivity.EXTRA_SELECT_FOLDER, true)
            putExtra(
                FileManageActivity.EXTRA_START_DIR,
                Environment.getExternalStorageDirectory().absolutePath
            )
        }
        if (hasSharedStorageAccess(context)) {
            appFolderPicker.launch(intent)
        } else {
            PermissionsCompat.Builder()
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_local_perm_request_storage)
                .onGranted { appFolderPicker.launch(intent) }
                .request()
        }
    }

    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        startAction = {
            startAction?.invoke()
            if (systemTypefaces != null && onSelectSystemTypeface != null) {
                RoundDropdownMenu(
                    expanded = showTypefaceMenu,
                    onDismissRequest = { showTypefaceMenu = false },
                ) {
                    ProvideAppDensity {
                        systemTypefaces.forEachIndexed { index, name ->
                            RoundDropdownMenuItem(
                                text = name,
                                onClick = {
                                    onSelectSystemTypeface(index)
                                    showTypefaceMenu = false
                                    onDismissRequest()
                                },
                            )
                        }
                    }
                }
                MediumTonalButton(
                    onClick = { showTypefaceMenu = true },
                    icon = Icons.Default.TextFields,
                    contentDescription = stringResource(R.string.select_font),
                )
            }
        },
        endAction = {
            if (onSelectFontFolder != null) {
                RoundDropdownMenu(
                    expanded = showFolderMenu,
                    onDismissRequest = { showFolderMenu = false },
                ) {
                    ProvideAppDensity {
                        RoundDropdownMenuItem(
                            text = stringResource(R.string.sys_folder_picker),
                            onClick = {
                                showFolderMenu = false
                                onOpenFolderPicker()
                            },
                        )
                        RoundDropdownMenuItem(
                            text = stringResource(R.string.app_folder_picker),
                            onClick = {
                                showFolderMenu = false
                                launchAppFolderPicker()
                            },
                        )
                    }
                }
            }
            MediumTonalButton(
                onClick = {
                    if (onSelectFontFolder != null) {
                        showFolderMenu = true
                    } else {
                        onOpenFolderPicker()
                    }
                },
                icon = folderIcon,
                contentDescription = folderContentDescription
                    ?: stringResource(R.string.select_folder),
            )
        },
    ) {
        FontSelectGrid(
            folderState = folderState,
            selectedFontName = selectedFontName,
            onSelectFont = { doc ->
                onSelectFont(doc)
                onDismissRequest()
            },
            emptyText = emptyText,
        )
    }
}

/**
 * 内置文件夹选择器靠 File API 读取共享存储，需要“所有文件访问”（Android 11+）。
 * 没有权限时先走应用统一的授权入口，授权成功后再打开选择器，
 * 否则列表会是空的，用户只会看到“没有字体文件”。
 */
private fun hasSharedStorageAccess(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Permissions.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }
