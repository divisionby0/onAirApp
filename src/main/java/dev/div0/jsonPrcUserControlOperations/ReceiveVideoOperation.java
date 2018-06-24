package dev.div0.jsonPrcUserControlOperations;


import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.internal.ProtocolElements;

public class ReceiveVideoOperation {

    public ReceiveVideoOperation(final Transaction transaction, final Request<JsonObject> request, ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager){
        String senderName = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
        senderName = senderName.substring(0, senderName.indexOf("_"));

        String sdpOffer = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

        roomManager.subscribe(senderName, sdpOffer, participantRequest);
    }
}
