package com.aidestinymaster.app.entry

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aidestinymaster.app.R
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.app.ui.glassCardColors
import com.aidestinymaster.app.ui.glassModifier
import android.util.Log

@Composable
private fun EntryScaffold(title: String, desc: String, onStart: () -> Unit) {
    Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
        Column(Modifier.padding(0.dp)) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().padding(top = 3.dp).then(Modifier)
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(desc, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onStart) { Text(stringResource(id = R.string.entry_start)) }
            }
        }
    }
}

@Composable
fun BaziEntryScreen(activity: ComponentActivity, nav: NavController) {
    EntryScaffold(
        title = stringResource(id = R.string.entry_bazi_title),
        desc = stringResource(id = R.string.entry_bazi_desc),
    ) {
        Log.i("AIDM", "Entry start clicked: bazi")
        nav.navigate(Routes.ChartInput.replace("{kind}", "bazi"))
    }
}

@Composable
fun ZiweiEntryScreen(activity: ComponentActivity, nav: NavController) {
    EntryScaffold(
        title = stringResource(id = R.string.entry_ziwei_title),
        desc = stringResource(id = R.string.entry_ziwei_desc),
    ) {
        Log.i("AIDM", "Entry start clicked: ziwei")
        nav.navigate(Routes.ChartInput.replace("{kind}", "ziwei"))
    }
}

@Composable
fun AstroEntryScreen(activity: ComponentActivity, nav: NavController) {
    EntryScaffold(
        title = stringResource(id = R.string.entry_astro_title),
        desc = stringResource(id = R.string.entry_astro_desc),
    ) {
        Log.i("AIDM", "Entry start clicked: natal")
        nav.navigate(Routes.ChartInput.replace("{kind}", "natal"))
    }
}

@Composable
fun DesignEntryScreen(activity: ComponentActivity, nav: NavController) {
    EntryScaffold(
        title = stringResource(id = R.string.entry_design_title),
        desc = stringResource(id = R.string.entry_design_desc),
    ) {
        Log.i("AIDM", "Entry start clicked: design")
        nav.navigate(Routes.ChartInput.replace("{kind}", "design"))
    }
}

@Composable
fun IchingEntryScreen(activity: ComponentActivity, nav: NavController) {
    EntryScaffold(
        title = stringResource(id = R.string.entry_iching_title),
        desc = stringResource(id = R.string.entry_iching_desc),
    ) {
        Log.i("AIDM", "Entry start clicked: iching")
        nav.navigate(Routes.ChartInput.replace("{kind}", "iching"))
    }
}
