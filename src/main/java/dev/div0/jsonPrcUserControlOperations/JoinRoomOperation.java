package dev.div0.jsonPrcUserControlOperations;


import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.internal.ProtocolElements;
import org.kurento.room.rpc.ParticipantSession;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoinRoomOperation {

    private static final Logger log = LoggerFactory.getLogger(JoinRoomOperation.class);

    public JoinRoomOperation(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager) throws IOException, InterruptedException, ExecutionException {
        String roomName = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.JOINROOM_ROOM_PARAM);
        String userName = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.JOINROOM_USER_PARAM);
        boolean isOwner = JsonRpcUserControlUtil.getBooleanParam(request, "isOwner");

        JsonRpcUserControlUtil.printTransaction(transaction);
        JsonRpcUserControlUtil.printRequest(request);

        int totalParticipants = roomManager.totalParticipants(roomName);
        log.info("Join room request. Room name = "+roomName);
        System.out.println("isOwner = "+isOwner);
        log.info("total participants = "+totalParticipants);

        boolean dataChannels = false;
        if (request.getParams().has(ProtocolElements.JOINROOM_DATACHANNELS_PARAM)) {
            dataChannels = request.getParams().get(ProtocolElements.JOINROOM_DATACHANNELS_PARAM).getAsBoolean();
        }

        ParticipantSession participantSession = JsonRpcUserControlUtil.getParticipantSession(transaction);
        participantSession.setParticipantName(userName);
        participantSession.setRoomName(roomName);
        participantSession.setDataChannels(dataChannels);

        log.info("Joining room...");
        roomManager.joinRoom(userName, roomName, dataChannels, true, participantRequest, isOwner);
    }
}
