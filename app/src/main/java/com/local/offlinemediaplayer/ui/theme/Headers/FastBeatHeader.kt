import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

//@file:Suppress("UnusedParameter")

/**
 * Minimal, professional FastBeat header:
 * - No logo
 * - Brand name is always visible
 * - Search looks integrated but does nothing unless you pass onSearchClick
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastBeatHeader(
    modifier: Modifier = Modifier,
    searchPlaceholderText: String = "Search",
    onSearchClick: (() -> Unit)? = null, // keep null for "does nothing" right now
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "FastBeat",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )

                FastBeatSearchPlaceholder(
                    text = searchPlaceholderText,
                    enabled = (onSearchClick != null),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    )
}

@Composable
private fun FastBeatSearchPlaceholder(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .height(48.dp)
            .semantics {
                // A consistent announcement for TalkBack.
                // When you wire up real search navigation later, change this to "Search" and add a click action.
                contentDescription = if (enabled) "Search" else "Search (unavailable)"
                role = Role.Button
            },
        shape = shape,
        color = containerColor,
        contentColor = placeholderColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}


