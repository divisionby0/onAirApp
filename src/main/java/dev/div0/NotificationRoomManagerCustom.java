package dev.div0;

import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.KurentoClientSessionInfo;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.RoomException;
import org.kurento.room.internal.DefaultKurentoClientSessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class NotificationRoomManagerCustom extends NotificationRoomManager1{

    private static final Logger log = LoggerFactory.getLogger(NotificationRoomManagerCustom.class);

    public NotificationRoomManagerCustom(UserNotificationService notificationService, KurentoClientProvider kcProvider) {
        super(notificationService, kcProvider);
        System.out.println("Im NotificationRoomManagerCustom");
        //log.info("Im NotificationRoomManagerCustom");
    }


    public int totalParticipants(String roomName){
        int total = 0;
        Set<UserParticipant> roomParticipants = null;
        try{
            roomParticipants = getParticipants(roomName);

        }catch(RoomException exception){
            log.info(exception.getMessage());
        }

        if(roomParticipants!=null){
            total = roomParticipants.size();
        }
        return total;
    }
}
