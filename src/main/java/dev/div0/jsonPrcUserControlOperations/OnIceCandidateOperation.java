package dev.div0.jsonPrcUserControlOperations;


import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.internal.ProtocolElements;

public class OnIceCandidateOperation {

    public OnIceCandidateOperation(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager){
        String endpointName = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.ONICECANDIDATE_EPNAME_PARAM);
        String candidate = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.ONICECANDIDATE_CANDIDATE_PARAM);
        String sdpMid = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.ONICECANDIDATE_SDPMIDPARAM);
        int sdpMLineIndex = JsonRpcUserControlUtil.getIntParam(request, ProtocolElements.ONICECANDIDATE_SDPMLINEINDEX_PARAM);

        roomManager.onIceCandidate(endpointName, candidate, sdpMLineIndex, sdpMid, participantRequest);
    }
}
