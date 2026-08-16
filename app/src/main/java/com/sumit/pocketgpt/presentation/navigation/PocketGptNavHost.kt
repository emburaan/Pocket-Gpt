package com.sumit.pocketgpt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sumit.pocketgpt.presentation.chat.ChatRoute
import com.sumit.pocketgpt.presentation.conversations.ConversationListRoute

private const val CONVERSATION_LIST_ROUTE = "conversations"
private const val CHAT_ROUTE = "chat/{conversationId}"

@Composable
fun PocketGptNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = CONVERSATION_LIST_ROUTE) {
        composable(CONVERSATION_LIST_ROUTE) {
            ConversationListRoute(
                onConversationClick = { conversationId ->
                    navController.navigate("chat/$conversationId")
                },
            )
        }
        composable(
            route = CHAT_ROUTE,
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType }),
        ) {
            ChatRoute(onBack = { navController.popBackStack() })
        }
    }
}
