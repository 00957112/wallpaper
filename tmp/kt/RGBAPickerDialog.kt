package com.example.wallpaper

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class RGBAPickerDialogFragment(private val defaultColor: Int) : DialogFragment() {

    private lateinit var colorSeekBarR: SeekBar
    private lateinit var colorSeekBarG: SeekBar
    private lateinit var colorSeekBarB: SeekBar
    private lateinit var colorValueTextView: TextView

    private var onColorChangedListener: ((Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_rgbapicker, null)

        colorSeekBarR = view.findViewById(R.id.seekBarR)
        colorSeekBarG = view.findViewById(R.id.seekBarG)
        colorSeekBarB = view.findViewById(R.id.seekBarB)

        // 设置滑块的初始值为传递的默认颜色值
        val initialColor = defaultColor
        colorValueTextView = view.findViewById(R.id.textViewColorValue)
        updateColorValueText(initialColor)
        // 设置滑块的初始值
        colorSeekBarR.progress = Color.red(initialColor)
        colorSeekBarG.progress = Color.green(initialColor)
        colorSeekBarB.progress = Color.blue(initialColor)

        colorSeekBarR.setOnSeekBarChangeListener(createOnSeekBarChangeListener())
        colorSeekBarG.setOnSeekBarChangeListener(createOnSeekBarChangeListener())
        colorSeekBarB.setOnSeekBarChangeListener(createOnSeekBarChangeListener())


        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        // 设置对话框背景透明
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    private fun createOnSeekBarChangeListener(): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 获取滑块位置
                val red = colorSeekBarR.progress
                val green = colorSeekBarG.progress
                val blue = colorSeekBarB.progress

                // 更新颜色数值和预览
                val color = Color.rgb(red, green, blue)
                updateColorValueText(color)
                onColorChangedListener?.invoke(color)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    private fun updateColorValueText(color: Int) {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val colorValue = "R: $red\nG: $green\nB: $blue\n"
        colorValueTextView.text = colorValue
    }

    fun setOnColorChangedListener(listener: (Int) -> Unit) {
        onColorChangedListener = listener
    }
}



// 取消