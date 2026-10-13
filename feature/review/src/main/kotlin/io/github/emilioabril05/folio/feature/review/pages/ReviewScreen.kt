package io.github.emilioabril05.folio.feature.review.pages

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.emilioabril05.folio.core.designsystem.component.FolioPrimaryButton
import io.github.emilioabril05.folio.core.designsystem.theme.FolioSpacing
import io.github.emilioabril05.folio.core.domain.EncodedImage
import io.github.emilioabril05.folio.core.model.PageFilter
import io.github.emilioabril05.folio.feature.review.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GRID_COLUMNS = 2
private const val PAGE_ASPECT_RATIO = 0.75f
private val BadgeSize = 28.dp
private val CellBorder = 1.dp

/** Revisión sin estado: cuadrícula de páginas que se reordenan arrastrando, con girar, quitar y añadir. */
@Composable
fun ReviewScreen(
    uiState: ReviewUiState,
    onEvent: (ReviewEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = (uiState as? ReviewUiState.Ready)?.title.orEmpty(), onBack = onBack)
            when (uiState) {
                ReviewUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }

                ReviewUiState.Missing -> {
                    Text(
                        text = stringResource(R.string.review_missing),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(FolioSpacing.l),
                    )
                }

                is ReviewUiState.Ready -> {
                    PageGrid(state = uiState, onEvent = onEvent, modifier = Modifier.weight(1f))
                    BottomBar(state = uiState, onEvent = onEvent)
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.review_back))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}

@Composable
private fun PageGrid(
    state: ReviewUiState.Ready,
    onEvent: (ReviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val drag = remember { PageDragState() }
    val haptics = LocalHapticFeedback.current
    val pages by rememberUpdatedState(state.pages)
    val finishDrag = {
        val from = pages.indexOfFirst { it.page.id == drag.dragging }
        val to = gridState.targetIndex(from, drag.offset)
        if (from >= 0 && to != null && to != from) {
            onEvent(ReviewEvent.Move(from, to))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        drag.reset()
    }

    Column(modifier = modifier) {
        if (state.editFailed) {
            Text(
                text = stringResource(R.string.review_edit_failed),
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .padding(
                            horizontal = FolioSpacing.m,
                        ).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            state = gridState,
            contentPadding = PaddingValues(FolioSpacing.m),
            horizontalArrangement = Arrangement.spacedBy(FolioSpacing.m),
            verticalArrangement = Arrangement.spacedBy(FolioSpacing.m),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(state.pages, key = { _, item -> item.page.id.value }) { index, item ->
                PageCell(
                    item = item,
                    number = index + 1,
                    total = state.pages.size,
                    onEvent = onEvent,
                    modifier =
                        Modifier
                            .dragVisual(drag, item.page.id)
                            .dragToReorder(drag, item.page.id, haptics, finishDrag),
                )
            }
            item(key = ADD_CELL_KEY) { AddCell(onClick = { onEvent(ReviewEvent.AddPages) }) }
        }
    }
}

@Composable
private fun BottomBar(
    state: ReviewUiState.Ready,
    onEvent: (ReviewEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(FolioSpacing.m)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FolioSpacing.s),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            PageFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.commonFilter == filter,
                    onClick = { onEvent(ReviewEvent.ApplyFilterToAll(filter)) },
                    label = { Text(stringResource(filter.labelRes())) },
                    modifier = Modifier.heightIn(min = MinChipHeight),
                )
            }
        }
        FolioPrimaryButton(
            text = stringResource(R.string.review_send_pdf),
            supportingText = pluralStringResource(R.plurals.review_page_count, state.pages.size, state.pages.size),
            onClick = { onEvent(ReviewEvent.Export) },
            enabled = state.pages.isNotEmpty(),
            modifier = Modifier.padding(top = FolioSpacing.s),
        )
    }
}

private val MinChipHeight = 48.dp

private fun PageFilter.labelRes(): Int =
    when (this) {
        PageFilter.Original -> R.string.review_filter_original
        PageFilter.Document -> R.string.review_filter_document
        PageFilter.Grayscale -> R.string.review_filter_grayscale
        PageFilter.EnhancedColor -> R.string.review_filter_color
    }

@Composable
private fun PageCell(
    item: ReviewPage,
    number: Int,
    total: Int,
    onEvent: (ReviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BorderStroke(CellBorder, MaterialTheme.colorScheme.outlineVariant),
                    MaterialTheme.shapes.medium,
                ),
    ) {
        PageThumbnail(item, number, total, onEvent)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { onEvent(ReviewEvent.Rotate(item.page.id)) }) {
                Icon(
                    painterResource(R.drawable.ic_rotate_right),
                    contentDescription = stringResource(R.string.review_rotate_page, number),
                )
            }
            IconButton(onClick = { onEvent(ReviewEvent.Delete(item.page.id)) }) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.review_delete_page, number),
                )
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    item: ReviewPage,
    number: Int,
    total: Int,
    onEvent: (ReviewEvent) -> Unit,
) {
    val description = stringResource(R.string.review_page_description, number, total)
    val moveBefore = stringResource(R.string.review_move_before)
    val moveAfter = stringResource(R.string.review_move_after)
    val index = number - 1
    val actions =
        buildList {
            if (index > 0) {
                add(CustomAccessibilityAction(moveBefore) { onEvent(ReviewEvent.Move(index, index - 1)).let { true } })
            }
            if (index < total - 1) {
                add(CustomAccessibilityAction(moveAfter) { onEvent(ReviewEvent.Move(index, index + 1)).let { true } })
            }
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(PAGE_ASPECT_RATIO)
                .clickable { onEvent(ReviewEvent.OpenPage(item.page.id)) }
                .semantics {
                    contentDescription = description
                    customActions = actions
                },
    ) {
        val thumbnail by rememberDecoded(item.thumbnail)
        thumbnail?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(FolioSpacing.s),
            )
        } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier =
                Modifier
                    .padding(FolioSpacing.s)
                    .size(BadgeSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(top = FolioSpacing.xs)
                    .align(Alignment.TopStart),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AddCell(onClick: () -> Unit) {
    val label = stringResource(R.string.review_add_pages)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(PAGE_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .border(BorderStroke(CellBorder, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {},
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

// Decodifica el JPEG fuera del hilo principal.
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
