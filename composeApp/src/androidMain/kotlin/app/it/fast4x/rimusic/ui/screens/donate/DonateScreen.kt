package app.it.fast4x.rimusic.ui.screens.donate

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.utils.SecureApiConfig
import app.kreate.android.BuildConfig
import app.kreate.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun DonateScreen(onBackClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val appearance = LocalAppearance.current
    val colors = appearance.colorPalette
    val typography = appearance.typography
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        SupportReportDialog(onDismiss = { showReportDialog = false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background0)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(colors.background2, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.text)
            }
        }

        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.donate_support_title),
                    style = typography.xxl.copy(fontWeight = FontWeight.Bold),
                    color = colors.text
                )
                Text(
                    text = stringResource(R.string.donate_support_subtitle),
                    style = typography.s,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background2, RoundedCornerShape(8.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.donate_maintainer_pass),
                        style = typography.l.copy(fontWeight = FontWeight.Bold),
                        color = colors.text
                    )
                    Text(
                        text = stringResource(R.string.donate_amounts),
                        style = typography.m.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.donate_no_product),
                        style = typography.xs,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            DonationMethod(
                title = stringResource(R.string.donate_fourthwall_title),
                subtitle = stringResource(R.string.donate_fourthwall_subtitle),
                detail = stringResource(R.string.donate_fourthwall_detail),
                icon = { Icon(Icons.Outlined.CreditCard, null, tint = colors.accent) },
                onClick = { uriHandler.openUri("https://cyberghost-shop.fourthwall.com") }
            )
        }

        item {
            DonationMethod(
                title = stringResource(R.string.donate_mpesa_title),
                subtitle = stringResource(R.string.donate_mpesa_subtitle),
                detail = stringResource(R.string.donate_mpesa_detail),
                icon = { Icon(Icons.Outlined.Payments, null, tint = colors.accent) },
                onClick = { uriHandler.openUri("https://support-pal-global.lovable.app/") }
            )
        }

        item {
            DonationMethod(
                title = stringResource(R.string.donate_report_issue_title),
                subtitle = stringResource(R.string.donate_report_issue_subtitle),
                detail = stringResource(R.string.donate_report_issue_detail),
                icon = { Icon(Icons.Outlined.OpenInNew, null, tint = colors.accent) },
                onClick = { showReportDialog = true }
            )
        }

        item {
            HorizontalDivider(color = colors.background3)
            Text(
                text = stringResource(R.string.donate_thank_you),
                style = typography.s,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SupportReportDialog(onDismiss: () -> Unit) {
    val appearance = LocalAppearance.current
    val colors = appearance.colorPalette
    val typography = appearance.typography
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }
    val sentText = stringResource(R.string.donate_report_sent)
    val failedText = stringResource(R.string.donate_report_failed)
    val emailInvalid = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val canSend = !sending && name.isNotBlank() && message.isNotBlank() && !emailInvalid
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.text,
        unfocusedTextColor = colors.text,
        focusedContainerColor = colors.background2,
        unfocusedContainerColor = colors.background2,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.textSecondary,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.background3,
        cursorColor = colors.accent,
        errorTextColor = colors.text,
        errorContainerColor = colors.background2,
        errorLabelColor = colors.red,
        errorBorderColor = colors.red,
    )

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        containerColor = colors.background1,
        title = {
            Text(
                text = stringResource(R.string.donate_report_issue_title),
                style = typography.l.copy(fontWeight = FontWeight.Bold),
                color = colors.text
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.donate_report_no_logs),
                    style = typography.xs,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background2, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.donate_report_name)) },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    isError = emailInvalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    label = { Text(stringResource(R.string.donate_report_email)) },
                    supportingText = {
                        if (emailInvalid) {
                            Text(stringResource(R.string.donate_report_email_invalid), color = colors.red)
                        }
                    },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    minLines = 4,
                    label = { Text(stringResource(R.string.donate_report_message)) },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://github.com/cybruGhost/Cubic-Music/issues") },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(stringResource(R.string.donate_report_github_issues), color = colors.accent)
                }
                resultText?.let { text ->
                    Text(
                        text = text,
                        style = typography.xs,
                        color = if (resultIsError) colors.red else colors.accent
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSend,
                onClick = {
                    coroutineScope.launch {
                        sending = true
                        resultText = null
                        submitSupportReport(
                            name = name.trim(),
                            email = email.trim(),
                            message = message.trim()
                        ).fold(
                            onSuccess = {
                                resultIsError = false
                                resultText = it.ifBlank { sentText }
                                name = ""
                                email = ""
                                message = ""
                            },
                            onFailure = {
                                resultIsError = true
                                resultText = it.message?.takeIf { value -> value.isNotBlank() }
                                    ?: failedText
                            }
                        )
                        sending = false
                    }
                }
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.donate_report_send))
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !sending, onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = colors.textSecondary)
            }
        }
    )
}
private suspend fun submitSupportReport(
    name: String,
    email: String,
    message: String
): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("name", name)
                .put("email", email)
                .put("app", "Cubic Music")
                .put("version", BuildConfig.VERSION_NAME)
                .put("message", message)
                .toString()

            val connection = (URL(SecureApiConfig.supportReportEndpoint)
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 12_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            connection.disconnect()

            val json = runCatching { JSONObject(responseText) }.getOrNull()
            val success = responseCode in 200..299 && json?.optBoolean("success") == true
            if (success) {
                json?.optString("message")?.takeIf { it.isNotBlank() } ?: "Report sent."
            } else {
                throw IllegalStateException(
                    json?.optString("error")?.takeIf { it.isNotBlank() }
                        ?: "HTTP $responseCode"
                )
            }
        }
    }

@Composable
private fun DonationMethod(
    title: String,
    subtitle: String,
    detail: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val appearance = LocalAppearance.current
    val colors = appearance.colorPalette
    val typography = appearance.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background1, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.background3, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = title,
                style = typography.m.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text
            )
            Text(text = subtitle, style = typography.xs, color = colors.textSecondary)
            Text(
                text = detail,
                style = typography.xxs,
                color = colors.accent,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Icon(Icons.Outlined.OpenInNew, contentDescription = stringResource(R.string.open), tint = colors.textSecondary)
    }
}
