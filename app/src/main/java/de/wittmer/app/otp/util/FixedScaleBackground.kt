package de.wittmer.app.otp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.doOnLayout
import kotlin.math.ceil
import kotlin.math.min

/**
 * Screen acts as a window over a fixed-scale backdrop.
 *
 * [designWidthPx] is the portrait reference width (1080): rotating must not zoom
 * the shield. If the view still needs more canvas than the asset provides, the
 * bitmap is padded with its edge colours (no stretch of the scene, no tiling).
 */
fun ImageView.applyFixedScaleBackground(
    @DrawableRes sourceRes: Int,
    designWidthPx: Int = DEFAULT_DESIGN_WIDTH_PX,
) {
    doOnLayout { view ->
        val viewWidth = view.width
        val viewHeight = view.height
        if (viewWidth <= 0 || viewHeight <= 0) return@doOnLayout

        val opts = BitmapFactory.Options().apply { inScaled = false }
        val decoded = BitmapFactory.decodeResource(resources, sourceRes, opts)
            ?: return@doOnLayout
        val source = if (decoded.config == Bitmap.Config.ARGB_8888) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, false).also { decoded.recycle() }
        }

        val shortSide = min(viewWidth, viewHeight).toFloat()
        val scale = shortSide / designWidthPx.toFloat()

        val neededWidthPx = ceil(viewWidth / scale).toInt().coerceAtLeast(source.width)
        val neededHeightPx = ceil(viewHeight / scale).toInt().coerceAtLeast(source.height)
        val canvasBitmap = padToSize(source, neededWidthPx, neededHeightPx)

        scaleType = ImageView.ScaleType.MATRIX
        setImageBitmap(canvasBitmap)

        val scaledW = canvasBitmap.width * scale
        val scaledH = canvasBitmap.height * scale
        imageMatrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((viewWidth - scaledW) / 2f, (viewHeight - scaledH) / 2f)
        }
    }
}

/** Center [source] on a larger canvas filled with sampled edge colours. */
private fun padToSize(source: Bitmap, neededWidth: Int, neededHeight: Int): Bitmap {
    if (neededWidth <= source.width && neededHeight <= source.height) {
        return source
    }

    val left = source.getPixel(0, source.height / 2)
    val right = source.getPixel(source.width - 1, source.height / 2)
    val top = source.getPixel(source.width / 2, 0)
    val bottom = source.getPixel(source.width / 2, source.height - 1)

    val out = Bitmap.createBitmap(neededWidth, neededHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    val offsetX = (neededWidth - source.width) / 2f
    val offsetY = (neededHeight - source.height) / 2f
    val contentRight = offsetX + source.width
    val contentBottom = offsetY + source.height

    // Full base with top colour, then override regions.
    paint.color = top
    canvas.drawRect(0f, 0f, neededWidth.toFloat(), neededHeight.toFloat(), paint)

    if (offsetY > 0f) {
        paint.color = top
        canvas.drawRect(0f, 0f, neededWidth.toFloat(), offsetY, paint)
    }
    if (contentBottom < neededHeight) {
        paint.color = bottom
        canvas.drawRect(0f, contentBottom, neededWidth.toFloat(), neededHeight.toFloat(), paint)
    }
    if (offsetX > 0f) {
        paint.color = left
        canvas.drawRect(0f, offsetY, offsetX, contentBottom, paint)
        paint.color = right
        canvas.drawRect(contentRight, offsetY, neededWidth.toFloat(), contentBottom, paint)
    }

    canvas.drawBitmap(source, offsetX, offsetY, Paint(Paint.FILTER_BITMAP_FLAG))
    source.recycle()
    return out
}

private const val DEFAULT_DESIGN_WIDTH_PX = 1080
