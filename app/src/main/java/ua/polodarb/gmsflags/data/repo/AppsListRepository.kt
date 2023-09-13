package ua.polodarb.gmsflags.data.repo

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.flow
import ua.polodarb.gmsflags.GMSApplication
import ua.polodarb.gmsflags.data.AppInfo
import ua.polodarb.gmsflags.ui.screens.appsScreen.AppsScreenUiStates
import ua.polodarb.gmsflags.ui.screens.appsScreen.dialog.DialogUiStates

class AppsListRepository(
    private val context: Context
) {
    fun getAllInstalledApps() = flow<AppsScreenUiStates> {
        emit(AppsScreenUiStates.Loading)

        val gmsPackages = (context as GMSApplication).getRootDatabase().googlePackages
        val pm = context.packageManager

        val appInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }

        val filteredAppInfoList = appInfoList.asSequence()
            .filter { gmsPackages.contains(it.packageName) && it.packageName.contains("com.google") }
//            .filterNot { it.packageName == "com.google.android.gm" || it.packageName.contains("gms") }
            .map { AppInfo.create(pm, it) }
            .sortedBy { it.appName }
            .toList()

        if (filteredAppInfoList.isNotEmpty()) {
            emit(AppsScreenUiStates.Success(filteredAppInfoList))
        }
    }

    fun getListByPackages(pkgName: String) = flow<DialogUiStates> {
        val context = context as GMSApplication
        val list = context.getRootDatabase().getListByPackages(pkgName).filterNot {
            if (pkgName == "com.google.android.gm") {
                it.contains("gms")
            } else {
                false
            }
        }
        emit(DialogUiStates.Success(list))
    }

}