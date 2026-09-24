package com.imagesorter.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.imagesorter.data.db.DecisionDao
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val PRIVACY_URL = "https://github.com/Nispeter/Peakselect/blob/main/PRIVACY.md"
private const val SOURCE_URL = "https://github.com/Nispeter/Peakselect"

/** Ajustes: cada cambio se aplica y se guarda al instante. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    dao: DecisionDao,
    access: Permissions.Status,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("settings-list"),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item { SectionTitle("Gestos") }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Distancia para deslizar", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Cuánto hay que deslizar la foto para decidir: ${(settings.swipeThreshold * 100).roundToInt()}% del ancho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = settings.swipeThreshold,
                        onValueChange = { onChange(settings.copy(swipeThreshold = it)) },
                        valueRange = AppSettings.SWIPE_MIN..AppSettings.SWIPE_MAX,
                        steps = 7, // de 5 en 5 %
                        modifier = Modifier.testTag("swipe-slider"),
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text("Menos", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text("Más", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                SwitchRow(
                    "Invertir lados",
                    if (settings.swapSides) "Izquierda = conservar, derecha = borrar" else "Izquierda = borrar, derecha = conservar",
                    settings.swapSides,
                ) { onChange(settings.copy(swapSides = it)) }
            }
            item {
                SwitchRow(
                    "Decidir tocando la foto",
                    "Tocar una mitad de la foto decide. Desactívalo para decidir solo deslizando.",
                    settings.tapToDecide,
                ) { onChange(settings.copy(tapToDecide = it)) }
            }
            item { SwitchRow("Vibrar al decidir", null, settings.vibrate) { onChange(settings.copy(vibrate = it)) } }

            item { SectionTitle("Orden y contenido") }
            item { ChoiceRow("Más recientes primero", !settings.oldestFirst) { onChange(settings.copy(oldestFirst = false)) } }
            item { ChoiceRow("Más antiguas primero", settings.oldestFirst) { onChange(settings.copy(oldestFirst = true)) } }
            item {
                SwitchRow(
                    "Incluir videos",
                    "Si lo desactivas, las carpetas y los mazos muestran solo fotos.",
                    settings.includeVideos,
                ) { onChange(settings.copy(includeVideos = it)) }
            }

            item { SectionTitle("Apariencia") }
            ThemeMode.entries.forEach { mode ->
                item {
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> "Según el sistema"
                        ThemeMode.LIGHT -> "Claro"
                        ThemeMode.DARK -> "Oscuro"
                    }
                    ChoiceRow(label, settings.theme == mode) { onChange(settings.copy(theme = mode)) }
                }
            }

            item { SectionTitle("Mantenimiento") }
            item {
                ListItem(
                    headlineContent = { Text("Reiniciar revisadas") },
                    supportingContent = { Text("Lo que ya revisaste vuelve a aparecer en los mazos. No modifica ningún archivo.") },
                    modifier = Modifier.clickable { confirmReset = true },
                )
            }
            if (Permissions.supportsManageMedia) {
                item {
                    ListItem(
                        headlineContent = { Text("Gestión de multimedia") },
                        supportingContent = {
                            Text(
                                if (access.manageMedia) {
                                    "Activada: Android no pide confirmación en cada lote."
                                } else {
                                    "Desactivada: Android pide confirmación en cada lote. Toca para activarla."
                                },
                            )
                        },
                        modifier = Modifier.clickable { Permissions.openManageMedia(context) },
                    )
                }
            }

            item { SectionTitle("Acerca de") }
            item {
                val version = remember { appVersion(context) }
                ListItem(headlineContent = { Text("Peakselect") }, supportingContent = { Text("Versión $version") })
            }
            item {
                ListItem(
                    headlineContent = { Text("Política de privacidad") },
                    supportingContent = { Text("No recopila ni envía ningún dato") },
                    modifier = Modifier.clickable { openUrl(context, PRIVACY_URL) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Código fuente") },
                    supportingContent = { Text("github.com/Nispeter/Peakselect") },
                    modifier = Modifier.clickable { openUrl(context, SOURCE_URL) },
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Reiniciar revisadas?") },
            text = { Text("Las fotos y videos que ya revisaste y aplicaste volverán a aparecer en los mazos. No se modifica ningún archivo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        dao.resetReviewed()
                        snackbar.showSnackbar("Revisadas reiniciadas")
                    }
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        HorizontalDivider(Modifier.padding(top = 8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"

/** Abre el enlace en el navegador. La app no tiene acceso a Internet: lo abre el navegador. */
fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // Sin navegador instalado: no hay nada que abrir.
    }
}
