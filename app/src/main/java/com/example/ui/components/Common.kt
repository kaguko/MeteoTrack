package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colours for content drawn on top of the weather gradient. */
object Glass {
    /** A dark scrim keeps white text above AA contrast on every gradient (unlike a white tint). */
    val Fill = Color(0x38000000)
    val FillStrong = Color(0x4D000000)
    val Border = Color(0x24FFFFFF)
    val OnGlass = Color.White
    val OnGlassMuted = Color(0xD1FFFFFF) // 82 %
    val Accent = Color(0xFFFFD66B)
}

/** Maximum readable width on tablets / landscape; content is centred inside the available space. */
const val MaxContentWidthDp = 640

@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = MaxContentWidthDp.dp).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Glass.Fill,
        contentColor = Glass.OnGlass,
        border = BorderStroke(1.dp, Glass.Border)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Icon + title row used at the top of cards. The title is exposed to screen readers as a heading. */
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    tint: Color = Glass.OnGlass,
    textColor: Color = Glass.OnGlass,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.labelMedium, color = Glass.OnGlassMuted)
        }
    }
}

/** Inline notice (offline, permission needed, refresh failed…). */
@Composable
fun InfoBanner(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                ) { Text(actionLabel) }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(52.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(4.dp))
            action()
        }
    }
}

/** Placeholder blocks shown on first load instead of a bare spinner. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = modifier.alpha(pulse),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBlock(width = 160.dp, height = 84.dp)
        SkeletonBlock(width = 120.dp, height = 24.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 96.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 140.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 220.dp)
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .background(Glass.Fill, RoundedCornerShape(20.dp))
    )
}

/**
 * Minimal Markdown renderer for suggestion text: `**bold**`, `- ` / `* ` bullets and `#` headings.
 * Anything else is shown verbatim.
 */
@Composable
fun SimpleMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        text.lines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> Spacer(modifier = Modifier.height(6.dp))
                line.startsWith("- ") || line.startsWith("* ") -> Row {
                    Text(text = "•", style = style, color = color, modifier = Modifier.width(18.dp))
                    Text(text = inlineBold(line.drop(2)), style = style, color = color, modifier = Modifier.weight(1f))
                }
                line.startsWith("#") -> Text(
                    text = inlineBold(line.trimStart('#').trim()),
                    style = style.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = Modifier.padding(top = 6.dp)
                )
                else -> Text(text = inlineBold(line), style = style, color = color)
            }
        }
    }
}

private val BoldRegex = Regex("\\*\\*(.+?)\\*\\*")

internal fun inlineBold(source: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in BoldRegex.findAll(source)) {
        append(source.substring(last, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
        last = match.range.last + 1
    }
    append(source.substring(last))
}

/**
 * Caps the system font scale for chrome with a fixed height (app-bar titles, navigation labels),
 * where 200 % text would otherwise be clipped. Scrollable content still follows the full setting.
 */
@Composable
fun CappedFontScale(max: Float = 1.3f, content: @Composable () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
            density = density.density,
            fontScale = density.fontScale.coerceAtMost(max)
        ),
        content = content
    )
}
