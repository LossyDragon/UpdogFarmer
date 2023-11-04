package com.steevsapps.idledaddy.handlers

import com.steevsapps.idledaddy.handlers.callbacks.PurchaseResponseCallback
import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2.CMsgClientPurchaseResponse

class PurchaseResponse : ClientMsgHandler() {

    override fun handleMsg(packetMsg: IPacketMsg) {
        when (packetMsg.msgType) {
            EMsg.ClientPurchaseResponse -> handlePurchaseResponse(packetMsg)
            else -> Unit
        }
    }

    private fun handlePurchaseResponse(packetMsg: IPacketMsg) {
        val purchaseResponse = ClientMsgProtobuf<CMsgClientPurchaseResponse.Builder>(
            CMsgClientPurchaseResponse::class.java,
            packetMsg
        )

        PurchaseResponseCallback(
            purchaseResponse.targetJobID,
            purchaseResponse.body
        ).also(getClient()::postCallback)
    }
}
