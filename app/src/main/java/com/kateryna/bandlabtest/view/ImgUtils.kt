package com.kateryna.bandlabtest.view

import android.graphics.*

/**
 * Created by kati4ka on 3/1/18.
 */
class ImgUtils {

    fun scaleToFitSize(bmp: Bitmap, containerWidth: Int, containerHeight: Int): Bitmap {
        val scaleFactor = getScaleForImg(containerWidth, containerHeight, bmp.width, bmp.height)
        val outWidth = bmp.width * scaleFactor
        val outHeight = bmp.height * scaleFactor
        return Bitmap.createScaledBitmap(bmp, outWidth.toInt(), outHeight.toInt(), false)
    }

    fun getScaleForImg(containerWidth: Int, containerHeight: Int, imgWidth: Int, imgHeight: Int): Float {
        val screenAspect = containerWidth.toFloat() / containerHeight
        val rectAspect = imgWidth.toFloat() / imgHeight.toFloat()
        return if (screenAspect > rectAspect)
            containerHeight.toFloat() / imgHeight
        else
            containerWidth.toFloat() / imgWidth
    }

    class TextLabel(val text: String, val x: Int, val y: Int, val height: Int)

    fun drawTextToBitmap(bmp: Bitmap, textSize: Float, textContainerWidth: Int, textContainerHeight: Int, labels: ArrayList<ImgUtils.TextLabel>): Bitmap {
        var bitmapConfig = bmp.config
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        val bitmap = bmp.copy(bitmapConfig, true)

        val scaleFactor = getScaleForImg(bitmap.width, bitmap.height, textContainerWidth, textContainerHeight)

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.YELLOW
        paint.textSize = textSize * scaleFactor

        labels.forEach {
            val bounds = Rect()
            paint.getTextBounds(it.text, 0, it.text.length, bounds)

            val x = it.x.toFloat() * scaleFactor
            val y = it.y.toFloat() * scaleFactor + bounds.height() + (it.height * scaleFactor - bounds.height()) / 2

            canvas.drawText(it.text, x, y, paint)
        }

        return bitmap
    }
}