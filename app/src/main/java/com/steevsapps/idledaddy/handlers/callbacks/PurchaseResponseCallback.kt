package com.steevsapps.idledaddy.handlers.callbacks

import `in`.dragonbra.javasteam.enums.EPurchaseResultDetail
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2.CMsgClientPurchaseResponse
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.types.JobID
import `in`.dragonbra.javasteam.types.KeyValue
import java.io.ByteArrayInputStream
import java.io.IOException

class PurchaseResponseCallback(
    jobID: JobID?,
    msg: CMsgClientPurchaseResponse.Builder
) : CallbackMsg() {

    val result: EResult = EResult.from(msg.eresult)

    val purchaseResultDetails: EPurchaseResultDetail =
        EPurchaseResultDetail.from(msg.purchaseResultDetails)

    val purchaseReceiptInfo: KeyValue = KeyValue()

    init {
        setJobID(jobID)

        try {
            purchaseReceiptInfo.tryReadAsBinary(ByteArrayInputStream(msg.purchaseReceiptInfo.toByteArray()))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
