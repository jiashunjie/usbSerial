package com.jt.usbserial


import java.nio.charset.Charset

/**
 * 将byte[]解析为所需数据
 */
object SerialDataUtil {
    private val TAG = "BleHelper"
    /**
     * 控制字符
     */
    val SOH = 0x01//报头开始
    val STX = 0x02//正文开始

    /**
     * 眼轴设备报头
     */
    private var DNT = byteArrayOf(0x44, 0x4E, 0x54)//DNT

    /**
     * 眼内压设备报头
     */
    private var TTT = byteArrayOf(0x54, 0x54, 0x54)//TTT

    /**
     * 验光设备三种报头
     */
    private var drm = byteArrayOf(0x64, 0x72, 0x6D)//drm
    private var DRM = byteArrayOf(0x44, 0x52, 0x4D)//DRM
    private var DKM = byteArrayOf(0x44, 0x4B, 0x4D)//DKM

    /**
     * R/L ID code(2)
     */
    private var OL = byteArrayOf(0x4F, 0x4C)//OL
    private var OR = byteArrayOf(0x4F, 0x52)//OR

    private var PL = byteArrayOf(0x50, 0x4C)//PL
    private var PR = byteArrayOf(0x50, 0x52)//PR

    private var DL = byteArrayOf(0x44, 0x4C)//DL
    private var DR = byteArrayOf(0x44, 0x52)//DR

    private var L = byteArrayOf(0x20, 0x4C)// L
    private var R = byteArrayOf(0x20, 0x52)// R

    private var SL = byteArrayOf(0x53, 0x4C)//SL
    private var SR = byteArrayOf(0x53, 0x52)//SR

    private var PD = byteArrayOf(0x56, 0x44)//PD

    private var AV = byteArrayOf(0x41, 0x56)//AV

    var isDNT = 1 // 眼内压
    var isTTT = 2  // 眼轴长度
    var isARK = 3// 验光

}