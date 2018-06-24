package dev.div0;

import com.google.gson.JsonObject;
import org.kurento.room.api.UserNotificationService;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.RoomException;
import org.kurento.room.internal.DefaultNotificationRoomHandler;
import org.kurento.room.internal.ProtocolElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

public class DefaultNotificationRoomHandlerCustom extends DefaultNotificationRoomHandler{
    private final Logger log = LoggerFactory.getLogger(DefaultNotificationRoomHandlerCustom.class);
    private UserNotificationService notifService;

    public DefaultNotificationRoomHandlerCustom(UserNotificationService notifService) {
        super(notifService);
        System.out.println("IM DefaultNotificationRoomHandlerCustom");
        this.notifService = notifService;
    }

    public void onRoomExistsResponseData(ParticipantRequest request, boolean isRoomExists, String roomName) {
        System.out.println("onRoomExistsResponseData getParticipantId="+request.getParticipantId());

        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("roomName", roomName);
        dataObject.addProperty("exists", isRoomExists);

        notifService.sendNotification(request.getParticipantId(), "onRoomExistanceResponse", dataObject);
        System.out.println("onRoomExistsResponseData SENT");
    }

    /*
    public void sendIsOwner(ParticipantRequest request, boolean isOwner){
        System.out.println("sendIsOwner to "+request.getParticipantId()+"  value="+isOwner);
        JsonObject notifParams = new JsonObject();
        if(isOwner){
            notifParams.addProperty("value", 1);
        }
        else{
            notifParams.addProperty("value", 0);
        }
        System.out.println("is owner "+isOwner+"   sending notification to client...");
        notifService.sendNotification(request.getParticipantId(),"isOwner",notifParams);
        notifService.sendNotification(request.getParticipantId(),"onIsOwnerStateChanged",notifParams);
    }
    */

    public void sendIsClientUriCorrectResponse(ParticipantRequest request, boolean isCorrect){
        JsonObject notifParams = new JsonObject();
        notifParams.addProperty("isCorrect", isCorrect);
        notifService.sendNotification(request.getParticipantId(),"onIsClientUriCorrect",notifParams);
    }

    public void sendPublisherExists(ParticipantRequest request, String roomName, boolean publisherExists){
        System.out.println("sendPublisherExists to "+request.getParticipantId());
        JsonObject notifParams = new JsonObject();
        notifParams.addProperty("roomName", roomName);
        notifParams.addProperty("value", publisherExists);
        notifService.sendNotification(request.getParticipantId(),"onRoomPublisherExists",notifParams);
    }

    public void onParticipantJoinedImpl(ParticipantRequest request, String roomName, String newUserName, Set<UserParticipant> existingParticipants, RoomException error, boolean isOwner) {
        System.out.println("onParticipantJoinedImpl isOwner = "+isOwner);
        //sendIsOwner(request, isOwner);

        super.onParticipantJoined(request, roomName, newUserName, existingParticipants, error);
        if(isOwner){
            System.out.println("existingParticipants = "+existingParticipants.size());
        }

        JsonObject notifParams = new JsonObject();
        notifParams.addProperty("total", existingParticipants.size());
        notifParams.addProperty("room", roomName);

        Iterator<UserParticipant> iterator = existingParticipants.iterator();
        while(iterator.hasNext()){
            UserParticipant participant = iterator.next();
            System.out.println("sending totalParticipants to "+participant.getUserName());
            notifService.sendNotification(participant.getParticipantId(),"totalParticipants",notifParams);
        }

        notifService.sendNotification(request.getParticipantId(),"totalParticipants",notifParams);

    }

    public void onOwnerLeft(ParticipantRequest request, String userName, Set<UserParticipant> remainingParticipants, RoomException error){
        if (error != null) {
            notifService.sendErrorResponse(request, null, error);
            return;
        }

        Iterator<UserParticipant> iterator = remainingParticipants.iterator();
        while(iterator.hasNext()){
            UserParticipant participant = iterator.next();
            System.out.println("sending onOwnerLeft to "+participant.getParticipantId());
            notifService.sendNotification(participant.getParticipantId(),"onOwnerLeft",null);
        }

        notifService.sendResponse(request, new JsonObject());
        notifService.closeSession(request);
    }

    @Override
    public void onParticipantLeft(ParticipantRequest request, String userName, Set<UserParticipant> remainingParticipants, RoomException error) {
        if (error != null) {
            notifService.sendErrorResponse(request, null, error);
            return;
        }

        JsonObject notifParams = new JsonObject();
        int total = remainingParticipants.size()-1;
        if(total<0){
            total = 0;
        }
        notifParams.addProperty("total", total);

        Iterator<UserParticipant> iterator = remainingParticipants.iterator();
        while(iterator.hasNext()){
            UserParticipant participant = iterator.next();
            notifService.sendNotification(participant.getParticipantId(),"totalParticipants",notifParams);
        }

        notifService.sendResponse(request, new JsonObject());
        notifService.closeSession(request);
    }

    @Override
    public void onUnpublishMedia(ParticipantRequest request, String publisherName, Set<UserParticipant> participants, RoomException error) {
        System.out.println("DefaultNotificationRoomHandlerCustom onUnpublishMedia");
        if (error != null) {
            notifService.sendErrorResponse(request, null, error);
            return;
        }
        notifService.sendResponse(request, new JsonObject());

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.PARTICIPANTUNPUBLISHED_NAME_PARAM, publisherName);

        for (UserParticipant participant : participants) {
            if (participant.getParticipantId().equals(request.getParticipantId())) {
                continue;
            } else {
                notifService.sendNotification(participant.getParticipantId(), ProtocolElements.PARTICIPANTUNPUBLISHED_METHOD, params);
            }
        }
    }


    public void sendTotalParticipants(){

    }
}
