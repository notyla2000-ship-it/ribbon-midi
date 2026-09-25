package com.example.ribbonmidi

import android.content.Context
import android.graphics.*
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var midiManager: MidiManager
    private var inputPort: MidiInputPort? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        midiManager = getSystemService(Context.MIDI_SERVICE) as MidiManager
        val devices = midiManager.devices
        for (info in devices) {
            if (info.type == MidiDeviceInfo.TYPE_USB) {
                midiManager.openDevice(info, { device ->
                    inputPort = device?.openInputPort(0)
                }, null)
                break
            }
        }

        val ribbonView = RibbonView(this) { norm ->
            val midiVal = ((norm + 1f) * 8191.5f).toInt().coerceIn(0, 16383)
            val lsb = (midiVal and 0x7F).toByte()
            val msb = ((midiVal shr 7) and 0x7F).toByte()
            inputPort?.send(byteArrayOf(0xE0.toByte(), lsb, msb), 0, 3)
        }

        val frame = FrameLayout(this)
        frame.addView(ribbonView)
        setContentView(frame)
    }

    override fun onDestroy() {
        super.onDestroy()
        inputPort?.close()
    }
}

class RibbonView(context: Context, val onPitchChange: (Float) -> Unit) : View(context) {
    private var touchX = -1f
    private var isTouching = false

    private val bgPaint = Paint().apply { color = Color.parseColor("#0a0a0f") }
    private val linePaint = Paint().apply { color = Color.parseColor("#334155"); strokeWidth = 3f }
    private val centerPaint = Paint().apply { color = Color.parseColor("#06b6d4"); strokeWidth = 6f }
    private val indicatorPaint = Paint().apply { color = Color.parseColor("#38bdf8"); strokeWidth = 8f }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f

        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawLine(w / 4f, 0f, w / 4f, h, linePaint)
        canvas.drawLine(3 * w / 4f, 0f, 3 * w / 4f, h, linePaint)
        canvas.drawLine(cx, 0f, cx, h, centerPaint)

        if (isTouching && touchX >= 0) {
            canvas.drawLine(touchX, 0f, touchX, h, indicatorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isTouching = true
                touchX = event.x.coerceIn(0f, width.toFloat())
                onPitchChange((touchX - cx) / cx)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                touchX = -1f
                onPitchChange(0f)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
