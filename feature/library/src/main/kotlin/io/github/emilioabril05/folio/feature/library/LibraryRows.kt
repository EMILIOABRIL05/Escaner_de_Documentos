package io.github.emilioabril05.folio.feature.library

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing
import io.github.emilioabril05.folio.core.domain.EncodedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val ThumbnailSize = 56.dp
private val ThumbnailCorner = 8.dp
private const val DAYS_IN_WEEK = 7L

@Composable
internal fun DocumentList(
    uiState: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onRename: (LibraryItem) -> Unit,
    onDeleteForever: (LibraryItem) -> Unit,
) {
    val today = LocalDate.now()
    val groups = uiState.items.groupBy { it.summary.updatedAt.group(today) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        DateGroup.entries.forEach { group ->
            val items = groups[group].orEmpty()
            if (items.isNotEmpty()) {
                item(key = group.name) {
                    Text(
                        text = stringResource(group.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .padding(
                                    start = FolioSpacing.m,
                                    top = FolioSpacing.m,
                                ).semantics { heading() },
                    )
                }
                items(items, key = { it.summary.id.value }) { item ->
                    if (uiState.isTrash) {
                        TrashRow(item, onEvent, onDeleteForever)
                    } else {
                        SwipeableRow(item, onEvent, onRename)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRow(
    item: LibraryItem,
    onEvent: (LibraryEvent) -> Unit,
    onRename: (LibraryItem) -> Unit,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) onEvent(LibraryEvent.Trash(item.summary.id))
                value == SwipeToDismissBoxValue.EndToStart
            },
        )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(FolioSpacing.m),
            ) {
                Icon(painterResource(R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.onError)
            }
        },
    ) {
        DocumentRow(item = item, onClick = { onEvent(LibraryEvent.Open(item.summary.id)) }) {
            RowMenu(item = item, onEvent = onEvent, onRename = onRename)
        }
    }
}

@Composable
private fun RowMenu(
    item: LibraryItem,
    onEvent: (LibraryEvent) -> Unit,
    onRename: (LibraryItem) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val copyTitle = stringResource(R.string.library_copy_title, item.summary.title)
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painterResource(R.drawable.ic_more_vert),
                stringResource(R.string.library_more_actions, item.summary.title),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.library_rename)) }, onClick = {
                open = false
                onRename(item)
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.library_duplicate)) }, onClick = {
                open = false
                onEvent(LibraryEvent.Duplicate(item.summary.id, copyTitle))
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.library_move_to_trash)) }, onClick = {
                open = false
                onEvent(LibraryEvent.Trash(item.summary.id))
            })
        }
    }
}

@Composable
private fun TrashRow(
    item: LibraryItem,
    onEvent: (LibraryEvent) -> Unit,
    onDeleteForever: (LibraryItem) -> Unit,
) {
    DocumentRow(item = item, onClick = null) {
        IconButton(onClick = { onEvent(LibraryEvent.Restore(item.summary.id)) }) {
            Icon(painterResource(R.drawable.ic_restore), stringResource(R.string.library_restore, item.summary.title))
        }
        IconButton(onClick = { onDeleteForever(item) }) {
            Icon(
                painterResource(R.drawable.ic_delete),
                stringResource(R.string.library_delete_forever_named, item.summary.title),
            )
        }
    }
}

@Composable
private fun DocumentRow(
    item: LibraryItem,
    onClick: (() -> Unit)?,
    actions: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val time = DateFormat.getTimeFormat(context).format(Date.from(item.summary.updatedAt))
    val pages = pluralStringResource(R.plurals.library_pages, item.summary.pageCount, item.summary.pageCount)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .heightIn(min = 72.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = FolioSpacing.m, vertical = FolioSpacing.s),
    ) {
        val thumbnail by rememberDecoded(item.thumbnail)
        Box(
            modifier =
                Modifier
                    .size(ThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCorner))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            thumbnail?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(FolioSpacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.summary.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$pages · $time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        actions()
    }
}

private enum class DateGroup(
    val labelRes: Int,
) {
    Today(R.string.library_group_today),
    ThisWeek(R.string.library_group_week),
    Earlier(R.string.library_group_earlier),
}

private fun Instant.group(today: LocalDate): DateGroup {
    val date = atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        date == today -> DateGroup.Today
        date.isAfter(today.minusDays(DAYS_IN_WEEK)) -> DateGroup.ThisWeek
        else -> DateGroup.Earlier
    }
}

@Composable
private fun rememberDecoded(image: EncodedImage?) =
    produceState<ImageBitmap?>(initialValue = null, image) {
        value =
            image?.let {
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)?.asImageBitmap()
                }
            }
    }
