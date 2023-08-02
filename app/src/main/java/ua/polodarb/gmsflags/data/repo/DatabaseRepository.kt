package ua.polodarb.gmsflags.data.repo

import android.content.Context
import kotlinx.coroutines.flow.flow
import ua.polodarb.gmsflags.di.GMSApplication
import ua.polodarb.gmsflags.ui.screens.ScreenUiStates
import ua.polodarb.gmsflags.ui.screens.flagChangeScreen.FlagChangeUiStates

class DatabaseRepository(
    private val context: Context
) {

    suspend fun getGmsPackages() = flow<ScreenUiStates> {
        val list = (context as GMSApplication).rootDatabase.gmsPackages

        if (list.isNotEmpty()) emit(ScreenUiStates.Success(list))
        else emit(ScreenUiStates.Error())

    }

    suspend fun getFlagsData(packageName: String) = flow<FlagChangeUiStates> {

        emit(FlagChangeUiStates.Loading)

        val boolFlagsMap = mutableMapOf<String, Boolean>()
        val intFlagsMap = mutableMapOf<String, String>()
        val floatFlagsMap = mutableMapOf<String, String>()
        val stringFlagsMap = mutableMapOf<String, String>()
        val extensionsFlagsMap = mutableMapOf<String, String>()

        val gmsApplication = context as GMSApplication

        val boolFlags = gmsApplication.rootDatabase.getBoolFlags(packageName)
        val intFlags = gmsApplication.rootDatabase.getIntFlags(packageName)
        val floatFlags = gmsApplication.rootDatabase.getFloatFlags(packageName)
        val stringFlags = gmsApplication.rootDatabase.getStringFlags(packageName)
        val extensionsFlags = gmsApplication.rootDatabase.getExtensionsFlags(packageName)

        if (boolFlags.isNotEmpty()) {
            for (flag in boolFlags) {
                val parts = flag.split("|")
                if (parts.size == 2) {
                    val text = parts[0]
                    val value = parts[1].toInt() == 1

                    boolFlagsMap[text] = value
                }
            }
        }

        if (intFlags.isNotEmpty()) {
            for (flag in intFlags) {
                val parts = flag.split("|")
                if (parts.size == 2) {
                    val text = parts[0]
                    val value = parts[1]

                    intFlagsMap[text] = value
                }
            }
        }

        if (floatFlags.isNotEmpty()) {
            for (flag in floatFlags) {
                val parts = flag.split("|")
                if (parts.size == 2) {
                    val text = parts[0]
                    val value = parts[1]

                    floatFlagsMap[text] = value
                }
            }
        }

        if (stringFlags.isNotEmpty()) {
            for (flag in stringFlags) {
                val parts = flag.split("|")
                if (parts.size == 2) {
                    val text = parts[0]
                    val value = parts[1]

                    stringFlagsMap[text] = value
                }
            }
        }

        if (extensionsFlags.isNotEmpty()) {
            for (flag in extensionsFlags) {
                val parts = flag.split("|")
                if (parts.size == 2) {
                    val text = parts[0]
                    val value = parts[1]

                    extensionsFlagsMap[text] = value
                }
            }
        }

        emit(FlagChangeUiStates.Success(PentagonMap(boolFlagsMap, intFlagsMap, floatFlagsMap, stringFlagsMap, extensionsFlagsMap)))
    }

    data class PentagonMap(
        val boolFlagsMap: Map<String, Boolean>,
        val intFlagsMap: Map<String, String>,
        val floatFlagsMap: Map<String, String>,
        val stringFlagsMap: Map<String, String>,
        val extensionsVal: Map<String, String>
    )

}