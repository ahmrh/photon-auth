package com.ahmrh.amryauth.common

import android.util.Log

//import com.chaquo.python.Python

object TOTPFunction {
//    fun generate(key: String): String{
//        val py = Python.getInstance()
//        val module = py.getModule( "TOTP" )
//        val getTOTP = module["getTOTP"]
//        val TOTP = getTOTP?.call(key.toByteArray())
//
//        return "$TOTP"
//    }
    fun generate(key: String): String{
        val otp = TOTP.getTOTP(key).toString()
        return otp.padStart(6, '0')
    }

}