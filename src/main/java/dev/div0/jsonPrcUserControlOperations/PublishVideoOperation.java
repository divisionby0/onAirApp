package dev.div0.jsonPrcUserControlOperations;

import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.internal.ProtocolElements;

public class PublishVideoOperation {
    public PublishVideoOperation(Transaction transaction, Request<JsonObject> request,
                                 ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager){

        String sdpOffer = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.PUBLISHVIDEO_SDPOFFER_PARAM);
        boolean doLoopback = JsonRpcUserControlUtil.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_DOLOOPBACK_PARAM);

        roomManager.publishMedia(participantRequest, sdpOffer, doLoopback);
    }
}
