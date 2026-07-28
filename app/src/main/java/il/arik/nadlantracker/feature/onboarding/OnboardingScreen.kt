package il.arik.nadlantracker.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.AppPrefs

@Composable
fun OnboardingScreen(
    onSeedResolved: (queryJson: String) -> Unit,
    onSearch: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.pendingQueryJson) {
        state.pendingQueryJson?.let { queryJson ->
            viewModel.onNavigated()
            AppPrefs.setOnboarded(context)
            onSeedResolved(queryJson)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 34.dp, bottom = 26.dp),
    ) {
        BrandMark()
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp),
        )

        FlowRow(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            viewModel.seeds.forEach { seed ->
                SeedChip(
                    name = seed.name,
                    city = seed.city,
                    resolving = state.resolvingName == seed.name,
                    onClick = { viewModel.pick(seed) },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        PrimaryButton(stringResource(R.string.onboarding_search_other)) {
            AppPrefs.setOnboarded(context)
            onSearch()
        }
        Text(
            stringResource(R.string.onboarding_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun BrandMark() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        modifier = Modifier.size(66.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inversePrimary,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun SeedChip(name: String, city: String, resolving: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(enabled = !resolving, onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (resolving) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            }
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(
                city,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
