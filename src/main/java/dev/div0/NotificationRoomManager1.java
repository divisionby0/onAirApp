package dev.div0;

import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaType;
import org.kurento.room.NotificationRoomManager;
import org.kurento.room.api.*;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.exception.RoomException;
import org.kurento.room.internal.DefaultKurentoClientSessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.Set;

// copy of NotificationRoomManager
// i cant get private internalManager(and its right) to extend NotificationRoomManager with custom joinRoom method
public class NotificationRoomManager1 {
    private final Logger log = LoggerFactory.getLogger(NotificationRoomManager1.class);

    private NotificationRoomHandler notificationRoomHandler;
    private RoomManagerCustom internalManager;

    public NotificationRoomManager1(UserNotificationService notificationService, KurentoClientProvider kcProvider) {
        super();
        System.out.println("creating new DefaultNotificationRoomHandlerCustom");
        this.notificationRoomHandler = new DefaultNotificationRoomHandlerCustom(notificationService);
        this.internalManager = new RoomManagerCustom(notificationRoomHandler, kcProvider);
    }

    public NotificationRoomManager1(NotificationRoomHandler notificationRoomHandler, KurentoClientProvider kcProvider) {
        super();
        System.out.println("received NotificationRoomHandler");
        this.notificationRoomHandler = notificationRoomHandler;
        this.internalManager = new RoomManagerCustom(notificationRoomHandler, kcProvider);
    }

    // ----------------- CLIENT-ORIGINATED REQUESTS ------------

    public void isClientUriCorrect(String clientUri, ParticipantRequest request, String availableUrisData){
        boolean isAvailable = false;
        String[] availableUris = availableUrisData.split(",");
        for(int i=0; i< availableUris.length; i++){
            if(availableUris[i].equals(clientUri)){
                isAvailable = true;
                break;
            }
        }
        DefaultNotificationRoomHandlerCustom notifyHandler = (DefaultNotificationRoomHandlerCustom)notificationRoomHandler;
        notifyHandler.sendIsClientUriCorrectResponse(request, isAvailable);
    }

    public void isRoomHasPublisher(String roomName, ParticipantRequest request){
        boolean publisherExists = internalManager.isPublisherExists(roomName);

        System.out.println("publisherExists = "+publisherExists);

        DefaultNotificationRoomHandlerCustom notifyHandler = (DefaultNotificationRoomHandlerCustom)notificationRoomHandler;

        notifyHandler.sendPublisherExists(request, roomName, publisherExists);
    }

    public void joinRoom(String userName, String roomName, boolean dataChannels, boolean webParticipant, ParticipantRequest request, boolean isOwner) {
        System.out.println("NotificationRoomManager1 joinRoom userName="+userName+"  roomName="+roomName);
        Set<UserParticipant> existingParticipants = null;

        System.out.println("isOwner = " +isOwner);

        DefaultNotificationRoomHandlerCustom notifyHandler = (DefaultNotificationRoomHandlerCustom)notificationRoomHandler;

        /*
        boolean publisherExists = internalManager.isPublisherExists(roomName);

        if(publisherExists && isOwner){
            // room already has publisher
            notifyHandler.sendPublisherAlreadyExists(request, roomName);
        }
        */

        try {
            KurentoClientSessionInfo kcSessionInfo = new DefaultKurentoClientSessionInfo(request.getParticipantId(), roomName);
            //notifyHandler.sendIsOwner(request, isOwner);

            existingParticipants = internalManager.joinRoom(userName, roomName, dataChannels, webParticipant, kcSessionInfo, request.getParticipantId(), isOwner);

        } catch (RoomException e) {
            log.warn("PARTICIPANT {}: Error joining/creating room {}", userName, roomName, e);
            notifyHandler.onParticipantJoinedImpl(request, roomName, userName, null, e, isOwner);
        }

        System.out.println("NotificationRoomManager1 existingParticipants="+existingParticipants);

        if (existingParticipants != null) {
            notifyHandler.onParticipantJoinedImpl(request, roomName, userName, existingParticipants, null, isOwner);
        }
    }

    public void leaveRoom(ParticipantRequest request) {
        System.out.println("leave room request ");
        String pid = request.getParticipantId();
        Set<UserParticipant> remainingParticipants = null;
        String roomName = null;
        String userName = null;

        boolean isOwner = false;

        try {
            roomName = internalManager.getRoomName(pid);
            userName = internalManager.getParticipantName(pid);

            System.out.println("roomName="+roomName);
            System.out.println("userName="+userName);
            System.out.println("roomOwnerName="+userName);

            UserLeftRoomData userLeftRoomData = internalManager.leaveRoom(pid);

            isOwner = userLeftRoomData.isOwner();

            System.out.println("left user is owner="+isOwner);

            remainingParticipants = userLeftRoomData.getRemainingParticipants();

        } catch (RoomException e) {
            log.warn("PARTICIPANT {}: Error leaving room {}", userName, roomName, e);
            notificationRoomHandler.onParticipantLeft(request, null, null, e);
        }
        System.out.println("remainingParticipants "+remainingParticipants.size());

        if (remainingParticipants != null) {
            if(isOwner){
                System.out.println("NEED TO SEND isOwner left the room");
                DefaultNotificationRoomHandlerCustom notifyHandler = (DefaultNotificationRoomHandlerCustom)notificationRoomHandler;
                notifyHandler.onOwnerLeft(request, userName, remainingParticipants, null);

                //notificationRoomHandler.onParticipantLeft(request, userName, remainingParticipants, null);
            }
            else{
                notificationRoomHandler.onParticipantLeft(request, userName, remainingParticipants, null);
            }
        }
    }

    public void publishMedia(ParticipantRequest request, boolean isOffer, String sdp,
                             MediaElement loopbackAlternativeSrc, MediaType loopbackConnectionType, boolean doLoopback,
                             MediaElement... mediaElements) {
        String pid = request.getParticipantId();
        String userName = null;
        Set<UserParticipant> participants = null;
        String sdpAnswer = null;
        try {
            userName = internalManager.getParticipantName(pid);
            sdpAnswer = internalManager.publishMedia(request.getParticipantId(), isOffer, sdp, loopbackAlternativeSrc, loopbackConnectionType, doLoopback, mediaElements);
            participants = internalManager.getParticipants(internalManager.getRoomName(pid));
        } catch (RoomException e) {
            log.warn("PARTICIPANT {}: Error publishing media", userName, e);
            notificationRoomHandler.onPublishMedia(request, null, null, null, e);
        }
        if (sdpAnswer != null) {
            notificationRoomHandler.onPublishMedia(request, userName, sdpAnswer, participants, null);
        }
    }

    public void publishMedia(ParticipantRequest request, String sdpOffer, boolean doLoopback, MediaElement... mediaElements) {
        this.publishMedia(request, true, sdpOffer, null, null, doLoopback, mediaElements);
    }

    public void unpublishMedia(ParticipantRequest request) {
        System.out.println("unpublishMedia");
        String pid = request.getParticipantId();
        String userName = null;
        Set<UserParticipant> participants = null;
        boolean unpublished = false;
        try {
            userName = internalManager.getParticipantName(pid);
            internalManager.unpublishMedia(pid);
            unpublished = true;
            participants = internalManager.getParticipants(internalManager.getRoomName(pid));
        } catch (RoomException e) {
            log.warn("PARTICIPANT {}: Error unpublishing media", userName, e);
            notificationRoomHandler.onUnpublishMedia(request, null, null, e);
        }
        if (unpublished) {
            notificationRoomHandler.onUnpublishMedia(request, userName, participants, null);
        }
    }

    public void subscribe(String remoteName, String sdpOffer, ParticipantRequest request) {
        //System.out.println("subscribe() sdpOffer="+sdpOffer);
        String pid = request.getParticipantId();
        String userName = null;
        String sdpAnswer = null;

        try {
            userName = internalManager.getParticipantName(pid);
            sdpAnswer = internalManager.subscribe(remoteName, sdpOffer, pid);
        } catch (RoomException e) {
            System.out.println("PARTICIPANT {}: Error subscribing to userName="+ userName+ "   remoteName"+remoteName+"  error:" + e);
            notificationRoomHandler.onSubscribe(request, null, e);
        }
        if (sdpAnswer != null) {
            System.out.println("subscribing to ParticipantId="+request.getParticipantId());
            notificationRoomHandler.onSubscribe(request, sdpAnswer, null);
        }
    }

    public void unsubscribe(String remoteName, ParticipantRequest request) {
        String pid = request.getParticipantId();
        String userName = null;
        boolean unsubscribed = false;
        try {
            userName = internalManager.getParticipantName(pid);
            internalManager.unsubscribe(remoteName, pid);
            unsubscribed = true;
        } catch (RoomException e) {
            log.warn("PARTICIPANT {}: Error unsubscribing from {}", userName, remoteName, e);
            notificationRoomHandler.onUnsubscribe(request, e);
        }
        if (unsubscribed) {
            notificationRoomHandler.onUnsubscribe(request, null);
        }
    }

    public void onIceCandidate(String endpointName, String candidate, int sdpMLineIndex, String sdpMid, ParticipantRequest request) {
        String pid = request.getParticipantId();
        String userName = null;
        try {
            userName = internalManager.getParticipantName(pid);
            internalManager.onIceCandidate(endpointName, candidate, sdpMLineIndex, sdpMid, request.getParticipantId());
            notificationRoomHandler.onRecvIceCandidate(request, null);
        } catch (RoomException e) {
            log.warn("PARTICIPANT {}: Error receiving ICE " + "candidate (epName={}, candidate={})", userName, endpointName, candidate, e);
            notificationRoomHandler.onRecvIceCandidate(request, e);
        }
    }

    public void sendRoomExistsResponse(ParticipantRequest request, boolean isRoomExists, String roomName){
        DefaultNotificationRoomHandlerCustom notifyHandler = (DefaultNotificationRoomHandlerCustom)notificationRoomHandler;
        notifyHandler.onRoomExistsResponseData(request, isRoomExists, roomName);
    }

    public void sendMessage(String message, String userName, String roomName, ParticipantRequest request) {
        log.debug("Request [SEND_MESSAGE] message={} ({})", message, request);
        try {
            if (!internalManager.getParticipantName(request.getParticipantId()).equals(userName)) {
                throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "Provided username '" + userName + "' differs from the participant's name");
            }
            if (!internalManager.getRoomName(request.getParticipantId()).equals(roomName)) {
                throw new RoomException(RoomException.Code.ROOM_NOT_FOUND_ERROR_CODE, "Provided room name '" + roomName
                        + "' differs from the participant's room");
            }
            notificationRoomHandler.onSendMessage(request, message, userName, roomName, internalManager.getParticipants(roomName), null);
        } catch (RoomException e) {
            log.warn("PARTICIPANT {}: Error sending message", userName, e);
            notificationRoomHandler.onSendMessage(request, null, null, null, null, e);
        }
    }
    public void addIncomingCall(String roomName, ParticipantRequest participantRequest){
        //notifyHandler.sendIsClientUriCorrectResponse(request, isAvailable);
    }

    // ----------------- APPLICATION-ORIGINATED REQUESTS ------------
    @PreDestroy
    public void close() {
        if (!internalManager.isClosed()) {
            internalManager.close();
        }
    }

    public Set<String> getRooms() {
        return internalManager.getRooms();
    }

    public Set<UserParticipant> getParticipants(String roomName) throws RoomException {
        return internalManager.getParticipants(roomName);
    }

    public Set<UserParticipant> getPublishers(String roomName) throws RoomException {
        return internalManager.getPublishers(roomName);
    }

    public Set<UserParticipant> getSubscribers(String roomName) throws RoomException {
        return internalManager.getSubscribers(roomName);
    }

    public Set<UserParticipant> getPeerPublishers(String participantId) throws RoomException {
        return internalManager.getPeerPublishers(participantId);
    }

    public Set<UserParticipant> getPeerSubscribers(String participantId) throws RoomException {
        return internalManager.getPeerSubscribers(participantId);
    }

    public void createRoom(KurentoClientSessionInfo kcSessionInfo) throws RoomException {
        internalManager.createRoom(kcSessionInfo);
    }

    public MediaPipeline getPipeline(String participantId) throws RoomException {
        return internalManager.getPipeline(participantId);
    }

    public void evictParticipant(String participantId) throws RoomException {
        UserParticipant participant = internalManager.getParticipantInfo(participantId);

        UserLeftRoomData userLeftRoomData = internalManager.leaveRoom(participantId);

        Set<UserParticipant> remainingParticipants = userLeftRoomData.getRemainingParticipants();

        // TODO need to info other participants if isOwner
        notificationRoomHandler.onParticipantLeft(participant.getUserName(), remainingParticipants);
        notificationRoomHandler.onParticipantEvicted(participant);
    }

    public void closeRoom(String roomName) throws RoomException {
        Set<UserParticipant> participants = internalManager.closeRoom(roomName);
        notificationRoomHandler.onRoomClosed(roomName, participants);
    }

    public String generatePublishOffer(String participantId) throws RoomException {
        return internalManager.generatePublishOffer(participantId);
    }

    public void addMediaElement(String participantId, MediaElement element) throws RoomException {
        internalManager.addMediaElement(participantId, element);
    }

    public void addMediaElement(String participantId, MediaElement element, MediaType type) throws RoomException {
        internalManager.addMediaElement(participantId, element, type);
    }

    public void removeMediaElement(String participantId, MediaElement element) throws RoomException {
        internalManager.removeMediaElement(participantId, element);
    }

    public void mutePublishedMedia(MutedMediaType muteType, String participantId)
            throws RoomException {
        internalManager.mutePublishedMedia(muteType, participantId);
    }

    public void unmutePublishedMedia(String participantId) throws RoomException {
        internalManager.unmutePublishedMedia(participantId);
    }

    public void muteSubscribedMedia(String remoteName, MutedMediaType muteType, String participantId) throws RoomException {
        internalManager.muteSubscribedMedia(remoteName, muteType, participantId);
    }

    public void unmuteSubscribedMedia(String remoteName, String participantId) throws RoomException {
        internalManager.unmuteSubscribedMedia(remoteName, participantId);
    }

    public RoomManagerCustom getRoomManager() {

        return internalManager;
    }

    public void addCaller(String userName, String participantId, String roomName) throws CallerAlreadyExistsException{
        internalManager.addCaller(userName, participantId, roomName);
    }

    protected Set<UserParticipant> internalJoinRoom(String userName, String roomName, boolean dataChannels, boolean webParticipant, KurentoClientSessionInfo kcSessionInfo, String participantId, boolean isOwner){
        System.out.println("internalJoinRoom isOwner="+isOwner);
        if(isOwner){
            System.out.println("need to send isOwner");
        }
        else{
            System.out.println("need to send notOwner");
        }
        Set<UserParticipant> existingParticipants = internalManager.joinRoom(userName, roomName, dataChannels, webParticipant, kcSessionInfo, participantId, isOwner);
        return existingParticipants;
    }

    protected void handleParticipantJoined(ParticipantRequest request, String roomName, String newUserName, Set<UserParticipant> existingParticipants, RoomException error, boolean isOwner){
        System.out.println("handleParticipantJoined isOwner="+isOwner);
        notificationRoomHandler.onParticipantJoined(request, roomName, newUserName, existingParticipants, error);
    }
}
