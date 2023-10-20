package ua.polodarb.gmsflags.ui.screens.suggestionsScreen

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.polodarb.gmsflags.GMSApplication
import ua.polodarb.gmsflags.data.remote.Resource
import ua.polodarb.gmsflags.data.remote.flags.FlagsApiService
import ua.polodarb.gmsflags.data.remote.flags.dto.FlagInfo
import ua.polodarb.gmsflags.data.remote.flags.dto.SuggestedFlagInfo
import ua.polodarb.gmsflags.data.repo.AppsListRepository
import ua.polodarb.gmsflags.data.repo.GmsDBRepository
import ua.polodarb.gmsflags.data.repo.interactors.MergeOverriddenFlagsInteractor
import ua.polodarb.gmsflags.data.repo.interactors.MergedOverriddenFlag
import ua.polodarb.gmsflags.ui.screens.UiStates
import java.io.File
import java.util.Collections

typealias SuggestionsScreenUiState = UiStates<List<SuggestedFlag>>

class SuggestionScreenViewModel(
    private val application: Application,
    private val repository: GmsDBRepository,
    private val appsRepository: AppsListRepository,
    private val flagsApiService: FlagsApiService,
    private val interactor: MergeOverriddenFlagsInteractor
) : ViewModel() {
    private val gmsApplication = application as GMSApplication

    private val _stateSuggestionsFlags =
        MutableStateFlow<SuggestionsScreenUiState>(UiStates.Loading())
    val stateSuggestionsFlags: StateFlow<SuggestionsScreenUiState> = _stateSuggestionsFlags.asStateFlow()

    private val usersList  = Collections.synchronizedList(mutableListOf<String>())

    private var rawSuggestedFlag = Collections.synchronizedList(emptyList<SuggestedFlagInfo>())

    init {
        initUsers()
        getAllOverriddenBoolFlags()
        initGmsPackages()
    }

    private fun initGmsPackages() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appsRepository.getAllInstalledApps().collectLatest {  }
            }
        }
    }

    fun updateFlagValue(newValue: Boolean, index: Int) {
        val currentState = _stateSuggestionsFlags.value
        if (currentState is UiStates.Success) {
            val updatedData = currentState.data.toMutableList()
            if (index != -1) {
                updatedData[index] = updatedData[index].copy(enabled = newValue)
                _stateSuggestionsFlags.value = currentState.copy(data = updatedData)
            }
        }
    }

    private fun initUsers() {
        usersList.clear()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.getUsers().collect {
                    usersList.addAll(it)
                }
            }
        }
    }

    private var overriddenFlags = mutableMapOf<String, MergedOverriddenFlag>()

    fun getAllOverriddenBoolFlags() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                    if (rawSuggestedFlag.isEmpty())
                        rawSuggestedFlag = loadSuggestedFlags()

                    gmsApplication.databaseInitializationStateFlow.collect { status ->
                        if (status.isInitialized) {
                            overriddenFlags = mutableMapOf()
                            rawSuggestedFlag.map { it.packageName }.forEach { pkg ->
                                if (overriddenFlags[pkg] == null) {
                                    overriddenFlags[pkg] = interactor.getMergedOverriddenFlagsByPackage(pkg)
                                }
                            }
                            _stateSuggestionsFlags.value = UiStates.Success(rawSuggestedFlag.map { flag ->
                                SuggestedFlag(
                                    flag = flag,
                                    enabled = flag.flags.firstOrNull {
                                        overriddenFlags[flag.packageName]?.boolFlag?.get(it.tag) != it.value &&
                                                overriddenFlags[flag.packageName]?.intFlag?.get(it.tag) != it.value &&
                                                overriddenFlags[flag.packageName]?.floatFlag?.get(it.tag) != it.value &&
                                                overriddenFlags[flag.packageName]?.stringFlag?.get(it.tag) != it.value
                                    } == null
                                )
                            })
                        }
                    }
            }
        }
    }

    private suspend fun loadSuggestedFlags(): List<SuggestedFlagInfo> {
        try {
            val localFlags = File(gmsApplication.filesDir.absolutePath + File.separator + "suggestedFlags.json")

            val flags = flagsApiService.getSuggestedFlags()
            if (flags is Resource.Success && flags.data != null) {
                localFlags.writeText(Json.encodeToString(flags.data))
                return flags.data
            }

            try {
                if (localFlags.exists())
                    return Json.decodeFromString(localFlags.readText())
            } catch (_: Exception) { }

            val pkgFlags = application.assets.open("suggestedFlags.json")
            val pkgContent = pkgFlags.bufferedReader().use { it.readText() }
            localFlags.writeText(pkgContent)
            return Json.decodeFromString(pkgContent)
        } catch (e: Exception) {
            _stateSuggestionsFlags.value = UiStates.Error(e)
            return emptyList()
        }

    }

    private fun clearPhenotypeCache(pkgName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val androidPkgName = repository.androidPackage(pkgName)
                Shell.cmd("am force-stop $androidPkgName").exec()
                Shell.cmd("rm -rf /data/data/$androidPkgName/files/phenotype").exec()
                if (pkgName.contains("finsky") || pkgName.contains("vending")) {
                    Shell.cmd("rm -rf /data/data/com.android.vending/files/experiment*").exec()
                    Shell.cmd("am force-stop com.android.vending").exec()
                }
                if (pkgName.contains("com.google.android.apps.photos")) {
                    Shell.cmd("rm -rf /data/data/com.google.android.apps.photos/shared_prefs/phenotype*").exec()
                    Shell.cmd("rm -rf /data/data/com.google.android.apps.photos/shared_prefs/com.google.android.apps.photos.phenotype.xml").exec()
                    Shell.cmd("am force-stop com.google.android.apps.photos").exec()
                }
                repeat(3) {
                    Shell.cmd("am start -a android.intent.action.MAIN -n $androidPkgName &").exec()
                    Shell.cmd("am force-stop $androidPkgName").exec()
                }
            }
        }
    }

    fun overrideFlag(
        packageName: String,
        name: String,
        flagType: Int = 0,
        intVal: String? = null,
        boolVal: String? = null,
        floatVal: String? = null,
        stringVal: String? = null,
        extensionVal: String? = null,
        committed: Int = 0
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteRowByFlagName(packageName, name)
                repository.overrideFlag(
                    packageName = packageName,
                    user = "",
                    name = name,
                    flagType = flagType,
                    intVal = intVal,
                    boolVal = boolVal,
                    floatVal = floatVal,
                    stringVal = stringVal,
                    extensionVal = extensionVal,
                    committed = committed
                )
                for (i in usersList) {
                    repository.overrideFlag(
                        packageName = packageName,
                        user = i,
                        name = name,
                        flagType = flagType,
                        intVal = intVal,
                        boolVal = boolVal,
                        floatVal = floatVal,
                        stringVal = stringVal,
                        extensionVal = extensionVal,
                        committed = committed
                    )
                }
                clearPhenotypeCache(packageName)
            }
        }
    }

    fun resetSuggestedFlagValue(packageName: String, flags: List<FlagInfo>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                flags.forEach {
                    repository.deleteRowByFlagName(packageName, it.tag)
                }
            }
        }
    }

}

