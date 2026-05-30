package com.scrimslegends.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource

enum class ReportReason(val label: String) {
    TOXICITY("Toxic Behavior"),
    CHEATING("Cheating / Hacking"),
    NO_SHOW("Did Not Show Up"),
    HARASSMENT("Harassment"),
    OTHER("Other")
}

enum class UserReportReason(val label: String) {
    SCAM("Scam / Fraud"),
    SUSPICIOUS("Suspicious Behavior"),
    INAPPROPRIATE("Inappropriate Content"),
    HARASSMENT("Harassment"),
    CHEATING("Cheating"),
    OTHER("Other")
}

@Composable
fun ReportDialog(
    targetName: String,
    reasons: List<String> = ReportReason.values().map { it.label },
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavy),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = ErrorRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.report_player),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        Text(
                            text = targetName,
                            fontSize = 14.sp,
                            color = LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reason selection
                Text(
                    text = stringResource(R.string.reason),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
                Spacer(modifier = Modifier.height(10.dp))

                reasons.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Surface(
                        onClick = { selectedReason = reason },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ErrorRed.copy(alpha = 0.15f) else White.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        color = if (isSelected) ErrorRed else Color.Transparent,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .then(
                                        if (!isSelected) Modifier.background(
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(50)
                                        ) else Modifier
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = reason,
                                fontSize = 14.sp,
                                color = if (isSelected) White else LightGray.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = stringResource(R.string.details_optional),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(stringResource(R.string.describe_what_happened), color = LightGray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ErrorRed,
                        unfocusedBorderColor = White.copy(alpha = 0.2f),
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel), color = LightGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    GradientButton(
                        text = stringResource(R.string.submit_report),
                        onClick = {
                            selectedReason?.let { onSubmit(it, description) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        enabled = selectedReason != null
                    )
                }
            }
        }
    }
}
