package com.ahmrh.amryauth.ui.screen.auth

import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ahmrh.amryauth.common.TOTPFunction
import com.ahmrh.amryauth.common.UiState
import com.ahmrh.amryauth.data.local.database.Auth
import com.ahmrh.amryauth.ui.components.AuthItem
import com.ahmrh.amryauth.ui.theme.AmryAuthTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    navHostController: NavHostController? = null,
    googleCodeScanner: () -> Unit,
) {



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Photon Authenticator")
                },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
//                navHostController?.navigate(Screen.Scanner.route)

//                viewModel.insertAuth("some_random_token")
                googleCodeScanner()


            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
    ) { innerPadding ->

        Surface(
            modifier = Modifier.padding(innerPadding)
        ) {

            when (val uiState = viewModel.authsUiState.collectAsState().value) {
                is UiState.Idle -> {
                    LoadingView()
                }

                is UiState.Loading -> {
                    LoadingView()
                }

                is UiState.Success -> {
                    val auths = uiState.data
                    AuthList(auths, viewModel)
                }

                is UiState.Error -> {
                    ErrorView(uiState.errorMessage)
                }
            }

        }


    }
}

@Composable
fun LoadingView(){
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthList(
    list: List<Auth>,
    viewModel: AuthViewModel? = null,
) {
    LazyColumn(
        modifier = Modifier.padding( vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(list) { auth ->

            val dismissState = rememberDismissState()

            if (dismissState.isDismissed(direction = DismissDirection.EndToStart)) {
                viewModel?.deleteAuthById(auth.id)

                Toast.makeText(LocalContext.current, "Auth ${auth.username} Deleted", Toast.LENGTH_LONG).show()
            }
            SwipeToDismiss(
                state = dismissState,
                directions = setOf(
                    DismissDirection.EndToStart
                ),
                background = {
                    // this background is visible when we swipe.
                    // it contains the icon

                    // background color
                    val backgroundColor by animateColorAsState(
                        when (dismissState.targetValue) {
                            DismissValue.DismissedToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.surface
                        }, label = "Background Color"
                    )

                    // icon size
                    val iconScale by animateFloatAsState(
                        targetValue = if (dismissState.targetValue == DismissValue.DismissedToStart) 1.3f else 0.5f,
                        label = "Icon Size"
                    )

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color = backgroundColor)
                            .padding(end = 16.dp), // inner padding
                        contentAlignment = Alignment.CenterEnd // place the icon at the end (left)
                    ) {
                        Icon(
                            modifier = Modifier.scale(iconScale),
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint =  MaterialTheme.colorScheme.surface
                        )
                    }
                },
                dismissContent = {
                    var ticks by remember { mutableStateOf(0L) }
                    var token by remember { mutableStateOf(TOTPFunction.generate(auth.key))}
                    val coroutineScope = rememberCoroutineScope()

                    val maxTick = 30
                    LaunchedEffect(Unit) {
                        while(true) {
                            delay(1.seconds)
                            val time = System.currentTimeMillis() / 1000
                            ticks = time % maxTick

                            if(time % maxTick == 0L){
                                Log.d("MainActivity", "Token changed to $token at $ticks")
                                token = "Generating"
                                coroutineScope.launch{
                                    delay(1.seconds)
                                    token = TOTPFunction.generate(auth.key)
                                }
                            }
                        }
                    }
                    AuthItem(token = token, ticks = ticks, maxTick = maxTick, username = auth.username)
                }
            )
        }

    }
}


@Composable
fun ErrorView(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(errorMessage)
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AuthScreenPreview() {
    AmryAuthTheme {
    }
}