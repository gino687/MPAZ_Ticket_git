package com.fabiancg.mpazticket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {

            MaterialTheme { AppMPAZ() }
        }
    }
}



@Composable
fun AppMPAZ(vm: TicketViewModel = viewModel()) {
    val nav = rememberNavController()
    val salir: () -> Unit = {
        vm.logout()
        nav.navigate("login") { popUpTo("lista") { inclusive = true } }
    }
    NavHost(
        nav, startDestination = "login",
        // Transición entre pantallas: entra deslizándose con rebote (como el ejemplo "entrada_bounce").
        enterTransition = {
            slideInHorizontally(
                animationSpec = spring<IntOffset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) { it } + fadeIn(tween(400))
        },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(400)) },
        popExitTransition = { slideOutHorizontally(tween(400)) { it } + fadeOut(tween(300)) }
    ) {
        composable("login") {
            PantallaLogin(vm) { nav.navigate("lista") { popUpTo("login") { inclusive = true } } }
        }
        composable("lista") {
            PantallaLista(
                vm,
                onCrear = { nav.navigate("crear") },
                onAbrir = { nav.navigate("detalle/$it") },
                onSalir = salir
            )
        }
        composable("crear") { PantallaCrear(vm, onVolver = { nav.popBackStack("lista", inclusive = false) }) }
        composable("detalle/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
            PantallaDetalle(vm, it.arguments!!.getInt("id"), onVolver = { nav.popBackStack("lista", inclusive = false) })
        }
    }
}
