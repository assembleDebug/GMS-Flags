package ua.polodarb.gmsflags.ui.navigation

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ua.polodarb.gmsflags.R
import ua.polodarb.gmsflags.ui.screens.appsScreen.AppsScreen
import ua.polodarb.gmsflags.ui.screens.historyScreen.HistoryScreen
import ua.polodarb.gmsflags.ui.screens.savedScreen.SavedScreen
import ua.polodarb.gmsflags.ui.screens.suggestionsScreen.SuggestionsScreen

sealed class NavBarItem(
    @StringRes val title: Int,
    @DrawableRes val iconActive: Int,
    @DrawableRes val iconInactive: Int?,
    val screenRoute: String
) {
    data object Suggestions : NavBarItem(
        title = R.string.nav_bar_suggestions,
        iconActive = R.drawable.ic_navbar_suggestions_active,
        iconInactive = R.drawable.ic_navbar_suggestions_inactive,
        screenRoute = "suggestions"
    )

    data object Apps : NavBarItem(
        title = R.string.nav_bar_apps,
        iconActive = R.drawable.ic_navbar_apps,
        iconInactive = null,
        screenRoute = "apps"
    )

    data object Saved : NavBarItem(
        title = R.string.nav_bar_saved,
        iconActive = R.drawable.ic_save_active,
        iconInactive = R.drawable.ic_save_inactive,
        screenRoute = "saved"
    )

    data object History : NavBarItem(
        title = R.string.nav_bar_history,
        iconActive = R.drawable.ic_navbar_history,
        iconInactive = null,
        screenRoute = "history"
    )
}

val navBarItems = listOf(NavBarItem.Suggestions, NavBarItem.Apps, NavBarItem.Saved, NavBarItem.History)

internal sealed class ScreensDestination(var screenRoute: String) {

    fun createStringRoute(rootRoute: String) = "${rootRoute}/$screenRoute"

    data object Root : ScreensDestination("root")
    data object FlagChange : ScreensDestination("{flagChange}") {
        fun createRoute(flagChange: String): String {
            return "packages/$flagChange"
        }
    }

    data object Settings : ScreensDestination("settings")
    data object Packages : ScreensDestination("packages")
    data object Welcome : ScreensDestination("welcome")
    data object RootRequest : ScreensDestination("rootRequest")
}

@Composable
internal fun BottomBarNavigation( // Navigation realization for BottomBar
    modifier: Modifier = Modifier,
    parentNavController: NavController,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NavBarItem.Suggestions.screenRoute,
        modifier = modifier
    ) {
        composable(route = NavBarItem.Suggestions.screenRoute) {
            SuggestionsScreen(
                onSettingsClick = {
                    parentNavController.navigate(ScreensDestination.Settings.screenRoute)
                },
                onPackagesClick = {
                    parentNavController.navigate(ScreensDestination.Packages.screenRoute)
                }
            )
        }
        composable(route = NavBarItem.Apps.screenRoute) {
            AppsScreen(
                onSettingsClick = {
                    parentNavController.navigate(ScreensDestination.Settings.screenRoute)
                },
                onPackagesClick = {
                    parentNavController.navigate(ScreensDestination.Packages.screenRoute)
                },
                onPackageItemClick = {
                    parentNavController.navigate(
                        ScreensDestination.FlagChange.createRoute(Uri.encode(it))
                    )
                }
            )
        }
        composable(route = NavBarItem.Saved.screenRoute) {
            SavedScreen(
                onSettingsClick = {
                    parentNavController.navigate(ScreensDestination.Settings.screenRoute)
                },
                onPackagesClick = {
                    parentNavController.navigate(ScreensDestination.Packages.screenRoute)
                },
                onFlagClick = {
                    parentNavController.navigate(
                        ScreensDestination.FlagChange.createRoute(Uri.encode(it))
                    )
                }
            )
        }
        composable(route = NavBarItem.History.screenRoute) {
            HistoryScreen(
                onSettingsClick = {
                    parentNavController.navigate(ScreensDestination.Settings.screenRoute)
                },
                onPackagesClick = {
                    parentNavController.navigate(ScreensDestination.Packages.screenRoute)
                }
            )
        }
    }
}
