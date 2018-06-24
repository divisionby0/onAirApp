package dev.div0.jsonPrcUserControlOperations;

import dev.div0.NotificationRoomManagerCustom;
import org.kurento.room.exception.RoomException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaveRoomAfterConnClosedOperation {

    private static final Logger log = LoggerFactory.getLogger(LeaveRoomAfterConnClosedOperation.class);

    public LeaveRoomAfterConnClosedOperation(String sessionId, NotificationRoomManagerCustom roomManager){
        try {
            roomManager.evictParticipant(sessionId);
            log.info("Evicted participant with sessionId {}", sessionId);
        } catch (RoomException e) {
            log.warn("Unable to evict: {}", e.getMessage());
            log.trace("Unable to evict user", e);
        }
    }
}
