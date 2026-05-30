package com.scrimslegends.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.PlayerRole
import com.scrimslegends.app.ui.components.GradientButton
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onAddPlayer: (name: String, email: String, role: PlayerRole) -> Unit
) {
    var playerName by remember { mutableStateOf("") }
    var playerEmail by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(PlayerRole.MEMBER) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavy,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.add_player_to_team, teamName),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Name input
                OutlinedTextField(
                    value = playerName,
                    onValueChange = {
                        playerName = it
                        errorMsg = ""
                    },
                    label = { Text("Player Name", color = LightGray) },
                    placeholder = { Text("Enter player name", color = MidGray) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BluePrimary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = LightGray,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = GoldPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email input
                OutlinedTextField(
                    value = playerEmail,
                    onValueChange = {
                        playerEmail = it
                        errorMsg = ""
                    },
                    label = { Text("Email", color = LightGray) },
                    placeholder = { Text("player@email.com", color = MidGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = LightGray,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = GoldPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Role selection
                Text(
                    text = stringResource(R.string.role),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = LightGray
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(PlayerRole.MEMBER, PlayerRole.CO_LEADER).forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = {
                                Text(
                                    role.name.replace("_", " "),
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (role) {
                                    PlayerRole.CO_LEADER -> SuccessGreen.copy(alpha = 0.2f)
                                    else -> BluePrimary.copy(alpha = 0.2f)
                                },
                                selectedLabelColor = when (role) {
                                    PlayerRole.CO_LEADER -> SuccessGreen
                                    else -> BluePrimary
                                },
                                containerColor = White.copy(alpha = 0.1f),
                                labelColor = LightGray
                            ),
                            border = null
                        )
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.add_player),
                onClick = {
                    if (playerName.isBlank()) {
                        errorMsg = "Please enter a player name"
                        return@GradientButton
                    }
                    if (playerEmail.isBlank() || !playerEmail.contains("@")) {
                        errorMsg = "Please enter a valid email"
                        return@GradientButton
                    }
                    onAddPlayer(playerName, playerEmail, selectedRole)
                    onDismiss()
                },
                gradient = BlueGradient,
                height = 48.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MidGray)
            }
        }
    )
}
