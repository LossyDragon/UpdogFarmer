package com.steevsapps.idledaddy.handlers;

import androidx.annotation.NonNull;

import com.steevsapps.idledaddy.handlers.callbacks.PurchaseResponseCallback;

import in.dragonbra.javasteam.base.ClientMsgProtobuf;
import in.dragonbra.javasteam.base.IPacketMsg;
import in.dragonbra.javasteam.enums.EMsg;
import in.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2;
import in.dragonbra.javasteam.steam.handlers.ClientMsgHandler;

public class PurchaseResponse extends ClientMsgHandler {
    @Override
    public void handleMsg(@NonNull IPacketMsg packetMsg) {
        if (packetMsg.getMsgType() == EMsg.ClientPurchaseResponse) {
            handlePurchaseResponse(packetMsg);
        }
    }

    private void handlePurchaseResponse(IPacketMsg packetMsg) {
        final ClientMsgProtobuf<SteammessagesClientserver2.CMsgClientPurchaseResponse.Builder> purchaseResponse;
        purchaseResponse = new ClientMsgProtobuf<>(SteammessagesClientserver2.CMsgClientPurchaseResponse.class, packetMsg);
        getClient().postCallback(new PurchaseResponseCallback(purchaseResponse.getTargetJobID(), purchaseResponse.getBody()));
    }
}