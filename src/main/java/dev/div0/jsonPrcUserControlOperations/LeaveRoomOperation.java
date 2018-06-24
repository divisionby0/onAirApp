package dev.div0.jsonPrcUserControlOperations;


import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaveRoomOperation {

    private static final Logger log = LoggerFactory.getLogger(LeaveRoomOperation.class);

    public LeaveRoomOperation(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager){

        boolean exists = false;
        String pid = participantRequest.getParticipantId();
        // trying with room info from session
        String roomName = null;
        if (transaction != null) {
            roomName = JsonRpcUserControlUtil.getParticipantSession(transaction).getRoomName();
        }
        if (roomName == null) { // null when afterConnectionClosed
            log.warn("No room information found for participant with session Id {}. "
                    + "Using the admin method to evict the user.", pid);

            new LeaveRoomAfterConnClosedOperation(pid, roomManager);

        } else {
            // sanity check, don't call leaveRoom unless the id checks out
            for (UserParticipant part : roomManager.getParticipants(roomName)) {
                if (part.getParticipantId().equals(participantRequest.getParticipantId())) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                //log.debug("Participant with sessionId {} is leaving room {}", pid, roomName);
                roomManager.leaveRoom(participantRequest);
                log.info("Participant with sessionId {} has left room {}", pid, roomName);
            } else {
                log.warn("Participant with session Id {} not found in room {}. " + "Using the admin method to evict the user.", pid, roomName);

                new LeaveRoomAfterConnClosedOperation(pid, roomManager);
            }
        }
    }
}
