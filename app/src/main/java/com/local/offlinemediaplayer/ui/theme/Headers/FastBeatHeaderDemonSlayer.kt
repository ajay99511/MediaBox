import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastBeatHeaderDemonSlayer(
    modifier: Modifier = Modifier,
    searchPlaceholderText: String = "Search",
    onSearchClick: (() -> Unit)? = null, // keep null for "does nothing" right now
) {
    // Demon Slayer inspired palette (kept tasteful + professional)
    val ink = Color(0xFF0B0B0F)          // deep charcoal
    val ink2 = Color(0xFF12121A)         // slightly lighter
    val ember = Color(0xFFE11D48)        // crimson/ember
    val emberSoft = Color(0x66E11D48)    // translucent ember

    val headerBrush = Brush.verticalGradient(
        colors = listOf(ink, ink2)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(headerBrush)
    ) {
        TopAppBar(
            // Make app bar transparent so our brush shows through
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
            ),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "FastBeat",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                        color = Color.White
                    )

                    DemonSlayerSearchPlaceholder(
                        text = searchPlaceholderText,
                        enabled = (onSearchClick != null),
                        ember = ember,
                        emberSoft = emberSoft,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        )

        // Thin ember accent line at the bottom (nice anime UI detail)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, ember, Color.Transparent)))
        )
    }
}

@Composable
private fun DemonSlayerSearchPlaceholder(
    text: String,
    enabled: Boolean,
    ember: Color,
    emberSoft: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)

    Surface(
        modifier = modifier
            .height(44.dp)
            .semantics {
                contentDescription = if (enabled) "Search" else "Search (unavailable)"
                role = Role.Button
            },
        shape = shape,
        color = Color(0x1AFFFFFF), // subtle frosted look on dark
        border = BorderStroke(1.dp, emberSoft),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Optional: if you want an icon, uncomment these lines + add icon imports.
            // Icon(
            //     imageVector = Icons.Rounded.Search,
            //     contentDescription = null,
            //     tint = ember
            // )

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xCCFFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.weight(1f))

            // Small ember dot (tiny “katana spark” vibe, still subtle)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ember)
            )
        }
    }
}
