package com.kateryna.bandlabtest.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.net.toUri
import androidx.view.forEach
import androidx.view.setPadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.jakewharton.rxbinding2.view.RxView
import com.kateryna.bandlabtest.R
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import java.security.MessageDigest


class MainActivity : AppCompatActivity() {

    companion object {
        private const val MAX_SIZE = 1024
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val TEXT_SIZE = 16
        private const val TEXT_HEIGHT = 28
    }
    var imgBitmap: Bitmap? = null
    private var storagePermissionSubject: PublishSubject<Boolean>? = null

    private fun loadImg() {
        imgBitmap = null
        image_container.removeViews(1, image_container.childCount - 1)
        Glide.with(applicationContext)
                .asBitmap()
                .load(image_url.text.toString())
                .apply(RequestOptions()
                        .transform(MultiTransformation(FitCenter(), MaxSizeTransformation())))
                .into(object: BitmapImageViewTarget(image) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        super.onResourceReady(resource, transition)
                        val scaleFactor = ImgUtils().getScaleForImg(image_container.width, image_container.height, resource.width, resource.height)
                        val params = text_labels_area.layoutParams as RelativeLayout.LayoutParams
                        params.width = (resource.width * scaleFactor).toInt()
                        params.height = (resource.height * scaleFactor).toInt()
                        imgBitmap = resource
                    }
                })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadImg()
        RxView.clicks(download).subscribe { loadImg() }

        //https://upload.wikimedia.org/wikipedia/commons/thumb/3/3d/LARGE_elevation.jpg/1600px-LARGE_elevation.jpg

        RxView.touches(image).filter { it.action == MotionEvent.ACTION_UP && imgBitmap != null }.subscribe {
            createEditText(it.x.toInt(), it.y.toInt())
        }
    }

    private fun getMaxMargins(padding: Int): Point {
        return Point(image.width - padding, image.height - padding)
    }

    private fun fixMargins(top: Int, left: Int): Point {
        val textHeight = (resources.displayMetrics.density * TEXT_HEIGHT).toInt()
        val max = getMaxMargins(textHeight)
        return Point(when {
            left > max.x -> max.x
            left < 0 -> 0
            else -> left
        }, when {
            top > max.y -> max.y
            top < 0 -> 0
            else -> top
        })
    }

    private fun createEditText(x: Int, y: Int) {
        val margins = fixMargins(x, y)
        val editText = EditText(this)
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.leftMargin = margins.x
        params.topMargin = margins.y
        editText.setTextColor(Color.YELLOW)
        editText.maxLines = 1
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE.toFloat())
        editText.setPadding(0)
        focusEditText(editText)
        image_container.addView(editText, params)
        setupListeners(editText)
    }

    private fun focusEditText(editText: EditText) {
        editText.post {
            editText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupListeners(editText: EditText) {

        var x = 0
        var y = 0
        var xOnDown = 0
        var yOnDown = 0
        var maxDiff = 0
        var time: Long = 0
        RxView.touches(editText).subscribe {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = it.rawX.toInt()
                    y = it.rawY.toInt()
                    xOnDown = x
                    yOnDown = y
                    maxDiff = 0
                    time = System.currentTimeMillis()
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = it.rawX.toInt()
                    val newY = it.rawY.toInt()
                    val diffX = newX - x
                    val diffY = newY - y

                    maxDiff = Math.max(maxDiff, Math.max(Math.abs(newX - xOnDown), Math.abs(newY - yOnDown)))

                    if (Math.abs(diffX) > 5 || Math.abs(diffY) > 5) {
                        editText.post {
                            val params = editText.layoutParams as RelativeLayout.LayoutParams
                            val margins = fixMargins(params.topMargin + diffY, params.leftMargin + diffX)
                            params.topMargin = margins.y
                            params.leftMargin = margins.x
                            editText.layoutParams = params
                            image_container.invalidate()
                        }

                        x = newX
                        y = newY
                    }
                }
                MotionEvent.ACTION_UP -> {
                    maxDiff = Math.max(maxDiff, Math.max(Math.abs(it.rawX.toInt() - xOnDown), Math.abs(it.rawY.toInt()  - yOnDown)))
                    val timeDiff = System.currentTimeMillis() - time
                    if (timeDiff > 400 && maxDiff < 5) {
                        image_container.removeView(editText)
                    } else {
                        focusEditText(editText)
                    }
                }
            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.share -> {
                permissions().subscribe {
                    val labelList = ArrayList<ImgUtils.TextLabel>()

                    image_container.forEach {
                        if (it is EditText) {
                            val params = it.layoutParams as RelativeLayout.LayoutParams
                            labelList.add(ImgUtils.TextLabel(it.text.toString(), params.leftMargin, params.topMargin, it.height))
                        }
                    }

                    val bitmapPath = MediaStore.Images.Media.insertImage(contentResolver,
                            ImgUtils().drawTextToBitmap(imgBitmap!!,TEXT_SIZE * resources.displayMetrics.density,
                                    text_labels_area.width, text_labels_area.height, labelList),
                            "bandLabTestImg", null)
                    val bitmapUri = bitmapPath.toUri()
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "image/png"
                    intent.putExtra(Intent.EXTRA_STREAM, bitmapUri)
                    startActivity(Intent.createChooser(intent, "Share"))
                }
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_options, menu)
        return true
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun permissions(): Observable<Boolean> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return Observable.just(true)
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), LOCATION_PERMISSION_REQUEST_CODE)
        }
        storagePermissionSubject?.onComplete()
        storagePermissionSubject = null
        storagePermissionSubject = PublishSubject.create<Boolean>()
        return storagePermissionSubject!!
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && storagePermissionSubject != null) {
            storagePermissionSubject?.onNext(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            storagePermissionSubject?.onComplete()
            storagePermissionSubject = null
        }
    }

    class MaxSizeTransformation: BitmapTransformation() {
        companion object {
            private const val ID = "com.bumptech.glide.transformations.MaxSizeTransformation"
            private val ID_BYTES = ID.toByteArray(charset(Key.STRING_CHARSET_NAME))
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(ID_BYTES);
        }

        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap =
                if (toTransform.width <= MAX_SIZE && toTransform.height <= MAX_SIZE)
                    toTransform
                else
                    ImgUtils().scaleToFitSize(toTransform, MAX_SIZE, MAX_SIZE)
    }
}
