package com.lisa.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lisa.app.R
import com.lisa.app.data.UserProfile
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(profile: UserProfile, onSave: (UserProfile) -> Unit) {
    val goals = remember(profile) { mutableStateListOf(*profile.goals.toTypedArray()) }
    val interests = remember(profile) { mutableStateListOf(*profile.interests.toTypedArray()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.profile_saved)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(text = stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall)

            TagSection(
                title = stringResource(R.string.profile_goals_title),
                description = stringResource(R.string.profile_goals_desc),
                tags = goals,
                onAdd = { goals.add(it) },
                onRemove = { goals.remove(it) },
            )

            TagSection(
                title = stringResource(R.string.profile_interests_title),
                description = stringResource(R.string.profile_interests_desc),
                tags = interests,
                onAdd = { interests.add(it) },
                onRemove = { interests.remove(it) },
            )

            Button(
                onClick = {
                    onSave(UserProfile(goals = goals.toList(), interests = interests.toList()))
                    coroutineScope.launch { snackbarHostState.showSnackbar(savedMessage) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_save))
            }
        }

        SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

@Composable
private fun TagSection(
    title: String,
    description: String,
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = description, style = MaterialTheme.typography.bodySmall)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(onClick = {
                val trimmed = input.trim()
                if (trimmed.isNotEmpty() && trimmed !in tags) {
                    onAdd(trimmed)
                    input = ""
                }
            }) {
                Text(stringResource(R.string.profile_add))
            }
        }

        tags.forEach { tag ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = tag, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemove(tag) }) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }
            }
        }
    }
}
