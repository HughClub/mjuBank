package com.hugh.outsourcing.bank_acs

import android.os.Bundle
import com.hugh.outsourcing.bank_acs.databinding.ActivityLoginBinding
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var number: String? = null
    companion object{
        const val shared = "Info"
        const val tag = "LoginActivity"
        const val noAccount = "请输入手机号"
        const val noPasswd = "请输入密码"
        const val noProtocol = "请同意协议"
    }

    private fun intentResult(token:String,data:String){
        intent.apply {
            putExtra("token",token)
            putExtra("user",data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)

        number = with(readPhone()){
            this?.let {
                binding.apply {
                    accountEditor.setText(it)
                    protocol.isChecked = true
                }
            }
            this
        }

        binding.login.setOnClickListener {
            if(login()){
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun login():Boolean{
        L.w(tag,"phone number:${binding.accountEditor.text}")
        if (binding.accountEditor.text.isNullOrEmpty()){
            this.showToast(noAccount)
            return false
        }
        L.w(tag,"passwd:${binding.passwdEditor.text}")
        if(binding.passwdEditor.text.isNullOrEmpty()){
            this.showToast(noPasswd)
            return false
        }
        if(!binding.protocol.isChecked){
            this.showToast(noProtocol)
            return false
        }

        this.showToast("正在登录中")

        val payload = Http.getLoginPayload(
            binding.accountEditor.text.toString(),
            binding.passwdEditor.text.toString()
        )
        Http.login(payload,object :okhttp3.Callback{
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    L.e(tag,"err msg: ${e.message}")
                    this@LoginActivity.showToast("登录失败，服务器出现错误")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val token = response.header("Authorization",null)
                val body = JSONObject(response.body!!.string())
                val code = body.getInt("code")
                L.d(tag,"code: $code")
                if (code != 200 || token == null){
                    L.e(tag,response.message)
                    runOnUiThread{
                        this@LoginActivity.showToast("登录失败，帐号或密码错误")
                    }
                }else{
                    savePhone(binding.accountEditor.text.toString())

                    intentResult(token,body.get("data").toString())
                    L.d(tag,body.get("data").toString())
                }
            }
        })
        return true
    }
}