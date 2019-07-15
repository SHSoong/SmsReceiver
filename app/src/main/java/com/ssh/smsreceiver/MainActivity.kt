package com.ssh.smsreceiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsMessage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var filter: IntentFilter? = null
    private var receiver: SmsReceiver? = null
    private var observer: SmsObserver? = null

    private val fileName = "msg_receiver.txt"
    private var type = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filter = IntentFilter()
        filter?.addAction("android.provider.Telephony.SMS_RECEIVED")

        tv_content.text = readFile()

        btnType.setOnClickListener {
            when (type) {
                1 -> {
                    type = 2
                    btnType.text = "SmsObserver"
                    initRegisterObserverr()
                    unregisterReceiver(receiver)
                }
                2 -> {
                    type = 1
                    btnType.text = "SmsReceiver"
                    initRegisterReceiver()
                    contentResolver.unregisterContentObserver(observer)
                }
            }
        }

        initRegisterReceiver()
    }

    private fun initRegisterReceiver() {
        receiver = SmsReceiver(updateUI)
        registerReceiver(receiver, filter)
    }

    private fun initRegisterObserverr() {
        observer = SmsObserver(updateUI)
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, observer)
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

    /**
     * 删除单个文件
     * @param   filePath    被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    override fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.isFile && file.exists()) {
            file.delete()
        } else false
    }

    private var updateUI: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 1) {
                val txt = msg.data.getString("msg")
                tv_content.text = "新短信：\n$txt"
//                deleteFile(filesDir.absolutePath + "/" + fileName)
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
            val msg = Message()
            msg.what = 1
            val b = Bundle()
            b.putString("msg", "$content\n")
            msg.data = b
            handler.sendMessage(msg)
        }
    }

    //一个继承自ContentObserver的监听器类
    internal inner class SmsObserver(private val handler: Handler) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean, uri: Uri) {
            //查询发送向箱中的短信
            // 第一遍 先执行content://sms/raw 第二遍则 uri.toString :content://sms/inbox
            if (uri.toString() == "content://sms/raw") {
                return
            }

            val cursor = contentResolver.query(Uri.parse("content://sms/inbox"),
                    null, null, null, "date desc")
            val content = StringBuffer()
            //遍历查询结果获取用户正在发送的短信
            if (cursor.moveToNext()) {
                content.append(cursor.getString(cursor.getColumnIndex("body")))
            }
            cursor.close()

            val msg = Message()
            msg.what = 1
            val b = Bundle()
            b.putString("msg", "$content\n")
            msg.data = b
            handler.sendMessage(msg)

            super.onChange(selfChange)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        contentResolver.unregisterContentObserver(observer)
    }

}
