package com.ssh.smsreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsMessage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var filter: IntentFilter? = null
    private var receiver: SmsReceiver? = null

    private val fileName = "msg_receiver.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filter = IntentFilter()
        filter?.addAction("android.provider.Telephony.SMS_RECEIVED")
        receiver = SmsReceiver(updateUI)
        registerReceiver(receiver, filter)

        tv_content.text = readFile()
    }

    private fun writeFile(txt: String) {
        try {
            val outPut = openFileOutput(fileName, Context.MODE_APPEND)
            val data: ByteArray = txt.toByteArray()
            outPut.write(data)
            outPut.flush()
            outPut.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readFile(): String {
        var txt = ""
        try {
            val input = openFileInput(fileName)
            val length = input.available()
            val buffer = ByteArray(length)
            input.read(buffer)
            txt = String(buffer)
            input.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return txt
    }

    private var updateUI: Handler = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 1) {
                val txt = msg.data.getString("msg")
                tv_content.text = txt
                writeFile(txt)
            }
        }
    }

    class SmsReceiver(private val handler: Handler) : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent) {
            val content = StringBuilder()
            val bundle = intent.extras
            val format = intent.getStringExtra("format")
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<Any>
                for (`object` in pdus) {
                    val message = SmsMessage.createFromPdu(`object` as ByteArray, format)
                    content.append(message.messageBody)//获取短信内容
                }
            }
            var text = ""
            text = "$text 新短信：$content\n"
            val msg = Message()
            msg.what = 1
            val b = Bundle()
            b.putString("msg", text)
            msg.data = b
            handler.sendMessage(msg)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

}
