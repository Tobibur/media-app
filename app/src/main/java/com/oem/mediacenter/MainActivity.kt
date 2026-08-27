package com.oem.mediacenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oem.mediacenter.data.ConnectionState
import com.oem.mediacenter.ui.BrowseScreen
import com.oem.mediacenter.ui.MediaCenterViewModel
import com.oem.mediacenter.ui.MiniPlayer
import com.oem.mediacenter.ui.NowPlayingScreen
import com.oem.mediacenter.ui.SourcesScreen
import com.oem.mediacenter.ui.theme.MediaCenterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MediaCenterApp
        setContent {
            MediaCenterTheme {
                MediaCenterAppRoot(
                    viewModel = viewModel(
                        factory = MediaCenterViewModel.factory(app.container.repository),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MediaCenterAppRoot(viewModel: MediaCenterViewModel) {
    val navController = rememberNavController()
    val sourcesState by viewModel.sourcesState.collectAsStateWithLifecycle()
    val browseState by viewModel.browseState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showMini = route != Routes.NOW_PLAYING &&
        connectionState is ConnectionState.Connected &&
        !nowPlaying.title.isNullOrBlank()

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SOURCES,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.SOURCES) {
                    SourcesScreen(
                        state = sourcesState,
                        onRefresh = viewModel::refreshSources,
                        onSourceClick = { source ->
                            viewModel.connect(source)
                            navController.navigate(Routes.browse(source.packageName))
                        },
                    )
                }
                composable(
                    route = Routes.BROWSE,
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
                ) {
                    BackHandler {
                        if (!viewModel.navigateBack()) {
                            navController.popBackStack()
                        }
                    }
                    BrowseScreen(
                        state = browseState,
                        connectionState = connectionState,
                        onBack = {
                            if (!viewModel.navigateBack()) {
                                navController.popBackStack()
                            }
                        },
                        onRetryRoot = viewModel::openRoot,
                        onNodeClick = { node ->
                            when {
                                node.isBrowsable -> viewModel.openFolder(node)
                                node.isPlayable -> {
                                    viewModel.play(node)
                                    navController.navigate(Routes.NOW_PLAYING)
                                }
                            }
                        },
                    )
                }
                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(
                        state = nowPlaying,
                        onBack = { navController.popBackStack() },
                        onPlayPause = viewModel::togglePlayPause,
                        onSkipNext = viewModel::skipNext,
                        onSkipPrevious = viewModel::skipPrevious,
                        onSeek = viewModel::seekTo,
                    )
                }
            }

            MiniPlayer(
                state = nowPlaying,
                visible = showMini,
                onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                onPlayPause = viewModel::togglePlayPause,
                onSkipNext = viewModel::skipNext,
                rootModifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private object Routes {
    const val SOURCES = "sources"
    const val BROWSE = "browse/{packageName}"
    const val NOW_PLAYING = "nowplaying"

    fun browse(packageName: String) = "browse/$packageName"
}
