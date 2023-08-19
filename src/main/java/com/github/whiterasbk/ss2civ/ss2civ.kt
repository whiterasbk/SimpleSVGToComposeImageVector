package com.github.whiterasbk.ss2civ

import android.content.Context
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.Reader
import java.util.Stack

private val unNecessaryPathAttributeTagNames = listOf("fill", "fill-opacity", "stroke-opacity", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin")
private const val commandSymbols = "MLHVCSQTAZ"
private val dPathRegex = Regex("([$commandSymbols${commandSymbols.lowercase()}])\\s*([0-9\\s.,-]+)?")

class DPathCommand (
    val type: String,
    val parameters: List<Float>
) {
    override fun toString(): String = "$type${parameters}"
}

data class ComposePath (
    var pathCommands: String,
    var name: String = "ComposePath",
    var fill: Brush? = SolidColor(Color.LightGray),
    var fillAlpha: Float = 1f,
    var stroke: Brush? = null,
    var strokeAlpha: Float = 1f,
    var strokeLineWidth: Float = 1f,
    var strokeLineCap: StrokeCap = StrokeCap.Butt,
    var strokeLineJoin: StrokeJoin = StrokeJoin.Bevel,
    var strokeLineMiter: Float = 1f,
    var pathFillType: PathFillType = DefaultFillType,
)

class SVGConvertException(message: String) : Exception(message)

fun Context.svgToImageVector(xml: Int) = svgXmlParserToImageVector(resources.getXml(xml))

fun svgToImageVector(svgString: String) = svgXmlParserToImageVector(
    XmlPullParserFactory.newInstance().newPullParser().apply {
        setInput(svgString.byteInputStream(charset("utf-8")), "utf-8")
    }
)

fun svgToImageVector(inputStream: InputStream, encoding: String = "utf-8") = svgXmlParserToImageVector(
    XmlPullParserFactory.newInstance().newPullParser().apply {
        setInput(inputStream, encoding)
    }
)

fun svgToImageVector(reader: Reader) = svgXmlParserToImageVector(
    XmlPullParserFactory.newInstance().newPullParser().apply {
        setInput(reader)
    }
)

fun svgXmlParserToImageVector(parser: XmlPullParser): ImageVector {
    var eventType = parser.eventType

    var width = 24f
    var height = 24f
    val paths: MutableList<ComposePath> = mutableListOf()
    val stack = Stack<String>()
    while (eventType != XmlPullParser.END_DOCUMENT) {

        when (eventType) {
            XmlPullParser.START_TAG -> {
                stack.push(parser.name)
                when (parser.name) {
                    "svg" -> {
                        val attrWidth: String? = parser.getAttributeValue(null, "width")
                        val attrHeight: String? = parser.getAttributeValue(null, "height")
                        val attrViewBox: String? = parser.getAttributeValue(null, "viewBox")

                        attrWidth?.let { width = it.toFloat() }
                        attrHeight?.let { height = it.toFloat() }
                    }

                    "path" -> {
                        if (stack.filter { it == "path" }.size == 1) {
                            val position = stack.joinToString(".")
                            val cp = ComposePath("", name = position)
                            val cfg = mutableMapOf<String, String>().apply {
                                this["d"] = parser.getAttributeValue(null, "d")
                                unNecessaryPathAttributeTagNames.forEach { attrName ->
                                    parser.getAttributeValue(null, attrName)?.let { attrValue ->
                                        this[attrName] = attrValue
                                    }
                                }
                            }
                            configPathLabelAttributes(cp, cfg)
                            paths += cp
                        }
                    }
                }
            }

            XmlPullParser.END_TAG -> {
                stack.pop()
            }
        }

        eventType = parser.next()
    }

    return pathsToImageVector(paths, width, height)
}

fun pathToImageVector(
    path: ComposePath,
    width: Float,
    height: Float,
    name: String = "ComposePath"
) = pathsToImageVector(mutableListOf(path), width, height, name)

fun pathLabelToImageVector(
    label: String,
    width: Float,
    height: Float,
    name: String = "ComposePath"
): ImageVector {
    val pathRegex = "<path\\s+([^>]+)>".toRegex()
    val attributeRegex = "\\s*([\\w-]+)=\"([^\"]*)\"".toRegex()
    val pathLabels = mutableListOf<Map<String, String>>()

    pathRegex.findAll(label).forEach { pathAttributes ->
        val attributes = mutableMapOf<String, String>()
        attributeRegex.findAll(pathAttributes.value).forEach { matchResult ->
            val attributeName = matchResult.groupValues[1]
            val attributeValue = matchResult.groupValues[2]
            attributes[attributeName] = attributeValue
        }
        pathLabels += attributes
    }

    return pathsToImageVector(
        pathLabels.map {
            ComposePath("").apply {
                configPathLabelAttributes(this, it)
            }
        },
        width, height, name
    )
}

fun pathsToImageVector (
    paths: List<ComposePath>,
    width: Float,
    height: Float,
    name: String = "ComposePath"
): ImageVector {

    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = width.dp,
        defaultHeight = height.dp,
        viewportWidth = width,
        viewportHeight = height
    )

    val buildDsl: ImageVector.Builder.() -> ImageVector.Builder = {
        var last: ImageVector.Builder? = null
        paths.forEachIndexed { index, path ->
            last = path(
                name = path.name + ":" + index,
                fill = path.fill,
                fillAlpha = path.fillAlpha,
                stroke = path.stroke,
                strokeAlpha = path.strokeAlpha,
                strokeLineWidth = path.strokeLineWidth,
                strokeLineCap = path.strokeLineCap,
                strokeLineJoin = path.strokeLineJoin,
                strokeLineMiter = path.strokeLineMiter,
                pathFillType = path.pathFillType,
                pathBuilder = { pathToPathBuilder(parseDPath(path.pathCommands)) }
            )
        }

        last ?: throw SVGConvertException("input svg should at least 1 <path> label")
    }

    return builder.buildDsl().build()
}

private fun parseDPath(d: String): List<DPathCommand> {
    val commands = mutableListOf<DPathCommand>()
    val matches = dPathRegex.findAll(d.trim())

    for (match in matches) {
        val command = match.groupValues[1]
        val parameters = match.groupValues[2]
            .split(",", " ", "\n")
            .filter { it.isNotBlank() }
            .map { it.toFloat() }
        commands += DPathCommand(command, parameters)
    }

    return commands
}

private fun PathBuilder.pathToPathBuilder(commands: List<DPathCommand>) {
    commands.forEach { cmd ->
        val p = cmd.parameters
        when (cmd.type) {
            "M" -> moveTo(p[0], p[1])
            "m" -> moveToRelative(p[0], p[1])

            "L" -> lineTo(p[0], p[1])
            "l" -> lineToRelative(p[0], p[1])

            "H" -> horizontalLineTo(p[0])
            "h" -> horizontalLineToRelative(p[0])

            "V" -> verticalLineTo(p[0])
            "v" -> verticalLineToRelative(p[0])

            "C" -> curveTo(p[0], p[1], p[2], p[3], p[4], p[5])
            "c" -> curveToRelative(p[0], p[1], p[2], p[3], p[4], p[5])

            "Q" -> quadTo(p[0], p[1], p[2], p[3])
            "q" -> quadToRelative(p[0], p[1], p[2], p[3])

            "S" -> reflectiveCurveTo(p[0], p[1], p[2], p[3])
            "s" -> reflectiveCurveToRelative(p[0], p[1], p[2], p[3])

            "T" -> reflectiveQuadTo(p[0], p[1])
            "t" -> reflectiveQuadToRelative(p[0], p[1])

            "A" -> arcTo(p[0], p[1], p[2], p[3] == 1f, p[4] == 1f, p[5], p[6])
            "a" -> arcToRelative(p[0], p[1], p[2], p[3] == 1f, p[4] == 1f, p[5], p[6])

            "Z", "z" -> close()
        }
    }
}

private fun hexToComposeColor(hexColor: String): Color {
    val cleanHex = hexColor.trimStart('#')

    val red = cleanHex.substring(0, 2).toInt(16) / 255.0f
    val green = cleanHex.substring(2, 4).toInt(16) / 255.0f
    val blue = cleanHex.substring(4, 6).toInt(16) / 255.0f

    val alpha = if (cleanHex.length == 8) {
        cleanHex.substring(6, 8).toInt(16) / 255.0f
    } else {
        1.0f
    }

    return if (alpha == 1.0f) Color(red, green, blue) else Color(red, green, blue, alpha)
}

private fun cssNamedColorToComposeColor(cssNamedColor: String): Color {
    return when (cssNamedColor.lowercase()) {
        "black" -> Color.Black
        "white" -> Color.White
        "red" -> Color.Red
        "green" -> Color.Green
        "blue" -> Color.Blue
        "yellow" -> Color.Yellow
        "pink" -> Color(0xFFFFC0CB)
        "purple" -> Color(0xFF800080)
        "orange" -> Color(0xFFFFA500)
        "gray" -> Color.Gray
        "", "none", "lightgray" -> Color.LightGray
        "darkgray" -> Color.DarkGray
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        else -> throw IllegalArgumentException("Unsupported CSS named color: $cssNamedColor")
    }
}

private fun configPathLabelAttributes(composePath: ComposePath, attr: Map<String, String>) {
    attr.forEach { (attr, value) ->
        when (attr) {
            "d" -> composePath.pathCommands = value
            "fill" -> composePath.fill = SolidColor(when {
                value.startsWith("#") -> hexToComposeColor(value)
                else ->  cssNamedColorToComposeColor(value)
            })

            "fill-opacity" -> composePath.fillAlpha = value.toFloat()

            "stroke-opacity" -> composePath.strokeAlpha = value.toFloat()

            "stroke" -> composePath.stroke = SolidColor( when {
                value.startsWith("#") -> hexToComposeColor(value)
                else ->  cssNamedColorToComposeColor(value)
            })

            "stroke-width" -> composePath.strokeLineWidth = value.toFloat()

            "stroke-linecap" -> composePath.strokeLineCap = when (value.lowercase()) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> StrokeCap.Butt
            }

            "stroke-linejoin" -> composePath.strokeLineJoin = when (value.lowercase()) {
                "round" -> StrokeJoin.Round
                "bevel" -> StrokeJoin.Bevel
                else -> StrokeJoin.Miter
            }
        }
    }
}