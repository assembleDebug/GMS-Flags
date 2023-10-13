package ua.polodarb.gmsflags.ui.screens.appsScreen

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.polodarb.gmsflags.data.AppInfo
import ua.polodarb.gmsflags.data.repo.AppsListRepository
import ua.polodarb.gmsflags.ui.screens.UiStates
import ua.polodarb.gmsflags.ui.screens.appsScreen.dialog.DialogUiStates

typealias AppInfoList = UiStates<List<AppInfo>>

class AppsScreenViewModel(
    private val repository: AppsListRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow<AppInfoList>(UiStates.Loading())
    val state: StateFlow<AppInfoList> = _state.asStateFlow()

    private val _dialogDataState =
        MutableStateFlow<DialogUiStates>(DialogUiStates.Loading)
    val dialogDataState: StateFlow<DialogUiStates> = _dialogDataState.asStateFlow()

    private val _dialogPackage = MutableStateFlow("")
    val dialogPackage: StateFlow<String> = _dialogPackage.asStateFlow()


    var searchQuery = mutableStateOf("")

    private val listAppsFiltered: MutableList<AppInfo> = mutableListOf()

    init {
        initAllInstalledApps()
    }

    fun setPackageToDialog(pkgName: String) {
        _dialogPackage.value = pkgName
    }

    fun setEmptyList() {
        _dialogDataState.value = DialogUiStates.Success(emptyList())
    }

    fun getListByPackages(pkgName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.getListByPackages(pkgName).collect { uiStates ->
                    when (uiStates) {
                        is DialogUiStates.Success -> {
                            _dialogDataState.value = DialogUiStates.Success(uiStates.data)
                        }

                        is DialogUiStates.Loading -> {
                            _dialogDataState.value = DialogUiStates.Loading
                        }

                        is DialogUiStates.Error -> {
                            _dialogDataState.value = DialogUiStates.Error()
                        }
                    }
                }
            }
        }
    }

    private fun initAllInstalledApps() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.getAllInstalledApps().collectLatest { uiStates ->
                    when (uiStates) {
                        is UiStates.Success -> {
                            listAppsFiltered.addAll(uiStates.data)
                            getAllInstalledApps()
                        }

                        is UiStates.Loading -> {
                            _state.value = UiStates.Loading()
                        }

                        is UiStates.Error -> {
                            _state.value = UiStates.Error()
                        }
                    }
                }
            }
        }
    }

    fun getAllInstalledApps() {
        if (listAppsFiltered.isNotEmpty()) {
            _state.value = UiStates.Success(
                listAppsFiltered.filter {
                    it.appName.contains(searchQuery.value, ignoreCase = true)
                }
            )
        }
    }
}
