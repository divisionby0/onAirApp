package dev.div0;

import dev.div0.caller.AddCaller;
import org.kurento.client.*;
import org.kurento.room.RoomManager;
import org.kurento.room.api.KurentoClientProvider;
import org.kurento.room.api.KurentoClientSessionInfo;
import org.kurento.room.api.MutedMediaType;
import org.kurento.room.api.RoomHandler;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.endpoint.SdpType;
import org.kurento.room.exception.RoomException;
import org.kurento.room.internal.Participant;
import org.kurento.room.internal.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RoomManagerCustom {
    private final Logger log = LoggerFactory.getLogger(RoomManagerCustom.class);

    private RoomHandler roomHandler;
    private KurentoClientProvider kcProvider;

    private final ConcurrentMap<String, RoomCustom> rooms = new ConcurrentHashMap<String, RoomCustom>();

    private volatile boolean closed = false;

    public RoomManagerCustom(RoomHandler roomHandler, KurentoClientProvider kcProvider) {
        super();
        System.out.println("Im RoomManagerCustom");
        this.roomHandler = roomHandler;
        this.kcProvider = kcProvider;
    }

    public boolean isPublisherExists(String roomName){
        RoomCustom room = rooms.get(roomName);
        if(room!=null){
            int totalPublishers = room.getActivePublishers();
            if(totalPublishers > 0){
                return true;
            }
        }
        else{
            return false;
        }
        return false;
    }

    public Set<UserParticipant> joinRoom(String userName, String roomName, boolean dataChannels, boolean webParticipant, KurentoClientSessionInfo kcSessionInfo, String participantId, boolean isOwner) throws RoomException {
        System.out.println("RoomManagerCustom joinRoom isOwner = "+isOwner+"  roomName="+roomName);

        RoomCustom room = rooms.get(roomName);

        if (room == null && kcSessionInfo != null) {
            createRoom(kcSessionInfo);
        }

        room = rooms.get(roomName);

        if (room == null) {
            log.warn("Room '{}' not found");
            throw new RoomException(RoomException.Code.ROOM_NOT_FOUND_ERROR_CODE, "Room '" + roomName + "' was not found, must be created before '" + userName + "' can join");
        }

        if (room.isClosed()) {
            log.warn("'{}' is trying to join room '{}' but it is closing", userName, roomName);
            throw new RoomException(RoomException.Code.ROOM_CLOSED_ERROR_CODE, "'" + userName + "' is trying to join room '" + roomName + "' but it is closing");
        }

        Set<UserParticipant> existingParticipants = getParticipants(roomName);
        room.join(participantId, userName, dataChannels, webParticipant);

        if(isOwner){
            System.out.println("Setting ownerName "+userName+ "  for room "+room.getName());
            room.setOwnerName(userName);
            System.out.println("Room "+room.getName()+"  ownerName = "+room.getOwnerName());
        }

        return existingParticipants;
    }

    public UserLeftRoomData leaveRoom(String participantId) throws RoomException {
        System.out.println("Request [LEAVE_ROOM] participantId="+ participantId);

        Participant participant = getParticipant(participantId);
        RoomCustom room = (RoomCustom)participant.getRoom();

        String roomName = room.getName();
        System.out.println("roomName = "+roomName);
        System.out.println("ownerName = "+room.getOwnerName());

        if (room.isClosed()) {
            log.warn("'{}' is trying to leave from room '{}' but it is closing", participant.getName(), roomName);
            throw new RoomException(RoomException.Code.ROOM_CLOSED_ERROR_CODE, "'" + participant.getName() + "' is trying to leave from room '" + roomName + "' but it is closing");
        }
        room.leave(participantId);

        Set<UserParticipant> remainingParticipants = null;

        try {
            remainingParticipants = getParticipants(roomName);

        } catch (RoomException e) {
            log.debug("Possible collision when closing the room '{}' (not found)");
            remainingParticipants = Collections.emptySet();
        }
        if (remainingParticipants.isEmpty()) {
            log.info("No more participants in room '{}', removing it and closing it", roomName);
            closeRoom(roomName);
        }

        boolean isOwner = participant.getName().equals(room.getOwnerName());

        UserLeftRoomData data = new UserLeftRoomData(remainingParticipants, isOwner);

        //return remainingParticipants;
        return data;
    }

    public String publishMedia(String participantId, boolean isOffer, String sdp,
                               MediaElement loopbackAlternativeSrc, MediaType loopbackConnectionType, boolean doLoopback,
                               MediaElement... mediaElements) throws RoomException {
        log.debug("Request [PUBLISH_MEDIA] isOffer={} sdp={} " + "loopbackAltSrc={} lpbkConnType={} doLoopback={} mediaElements={} ({})", isOffer, sdp,
                loopbackAlternativeSrc == null, loopbackConnectionType, doLoopback, mediaElements,
                participantId);

        SdpType sdpType = isOffer ? SdpType.OFFER : SdpType.ANSWER;
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        Room room = participant.getRoom();

        participant.createPublishingEndpoint();

        for (MediaElement elem : mediaElements) {
            participant.getPublisher().apply(elem);
        }

        String sdpResponse = participant.publishToRoom(sdpType, sdp, doLoopback,
                loopbackAlternativeSrc, loopbackConnectionType);
        if (sdpResponse == null) {
            throw new RoomException(RoomException.Code.MEDIA_SDP_ERROR_CODE, "Error generating SDP response for publishing user " + name);
        }

        room.newPublisher(participant);
        return sdpResponse;
    }

    public String publishMedia(String participantId, String sdp, boolean doLoopback, MediaElement... mediaElements) throws RoomException {
        return publishMedia(participantId, true, sdp, null, null, doLoopback, mediaElements);
    }

    public String publishMedia(String participantId, boolean isOffer, String sdp, boolean doLoopback,
                               MediaElement... mediaElements) throws RoomException {
        return publishMedia(participantId, isOffer, sdp, null, null, doLoopback, mediaElements);
    }

    public String generatePublishOffer(String participantId) throws RoomException {
        log.debug("Request [GET_PUBLISH_SDP_OFFER] ({})", participantId);

        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        Room room = participant.getRoom();

        participant.createPublishingEndpoint();

        String sdpOffer = participant.preparePublishConnection();
        if (sdpOffer == null) {
            throw new RoomException(RoomException.Code.MEDIA_SDP_ERROR_CODE,
                    "Error generating SDP offer for publishing user " + name);
        }

        room.newPublisher(participant);
        return sdpOffer;
    }

    public void unpublishMedia(String participantId) throws RoomException {
        log.debug("Request [UNPUBLISH_MEDIA] ({})", participantId);
        Participant participant = getParticipant(participantId);

        if (!participant.isStreaming()) {
            throw new RoomException(RoomException.Code.USER_NOT_STREAMING_ERROR_CODE, "Participant '" + participant.getName() + "' is not streaming media");
        }

        Room room = participant.getRoom();
        participant.unpublishMedia();
        room.cancelPublisher(participant);
    }

    public String subscribe(String remoteName, String sdpOffer, String participantId) throws RoomException {
        //System.out.println("Request [SUBSCRIBE] remoteName="+remoteName+"  sdpOffer="+ sdpOffer+"  participantId="+ participantId);

        Participant participant = getParticipant(participantId);
        String name = participant.getName();

        Room room = participant.getRoom();

        System.out.println("room = "+room.getName());

        Participant senderParticipant = room.getParticipantByName(remoteName);

        if (senderParticipant == null) {
            log.warn("PARTICIPANT {}: Requesting to recv media from user {} " + "in room {} but user could not be found", name, remoteName, room.getName());
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "User '" + remoteName + " not found in room '" + room.getName() + "'");
        }

        if (!senderParticipant.isStreaming()) {
            log.warn("PARTICIPANT {}: Requesting to recv media from user {} " + "in room {} but user is not streaming media", name, remoteName, room.getName());
            throw new RoomException(RoomException.Code.USER_NOT_STREAMING_ERROR_CODE, "User '" + remoteName + " not streaming media in room '" + room.getName() + "'");
        }

        String sdpAnswer = participant.receiveMediaFrom(senderParticipant, sdpOffer);
        if (sdpAnswer == null) {
            throw new RoomException(RoomException.Code.MEDIA_SDP_ERROR_CODE,
                    "Unable to generate SDP answer when subscribing '" + name + "' to '" + remoteName + "'");
        }
        return sdpAnswer;
    }

    public void unsubscribe(String remoteName, String participantId) throws RoomException {
        log.debug("Request [UNSUBSCRIBE] remoteParticipant={} ({})", remoteName, participantId);
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        Room room = participant.getRoom();
        Participant senderParticipant = room.getParticipantByName(remoteName);
        if (senderParticipant == null) {
            log.warn("PARTICIPANT {}: Requesting to unsubscribe from user {} "
                    + "in room {} but user could not be found", name, remoteName, room.getName());
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "User " + remoteName
                    + " not found in room " + room.getName());
        }
        participant.cancelReceivingMedia(remoteName);
    }

    public void onIceCandidate(String endpointName, String candidate, int sdpMLineIndex,
                               String sdpMid, String participantId) throws RoomException {
        log.debug(
                "Request [ICE_CANDIDATE] endpoint={} candidate={} " + "sdpMLineIdx={} sdpMid={} ({})",
                endpointName, candidate, sdpMLineIndex, sdpMid, participantId);
        Participant participant = getParticipant(participantId);
        participant.addIceCandidate(endpointName, new IceCandidate(candidate, sdpMid, sdpMLineIndex));
    }

    public void addMediaElement(String participantId, MediaElement element) throws RoomException {
        addMediaElement(participantId, element, null);
    }

    public void addMediaElement(String participantId, MediaElement element, MediaType type)
            throws RoomException {
        log.debug("Add media element {} (connection type: {}) to participant {}", element.getId(),
                type, participantId);
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        if (participant.isClosed()) {
            throw new RoomException(RoomException.Code.USER_CLOSED_ERROR_CODE, "Participant '" + name
                    + "' has been closed");
        }
        participant.shapePublisherMedia(element, type);
    }

    public void removeMediaElement(String participantId, MediaElement element) throws RoomException {
        log.debug("Remove media element {} from participant {}", element.getId(), participantId);
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        if (participant.isClosed()) {
            throw new RoomException(RoomException.Code.USER_CLOSED_ERROR_CODE, "Participant '" + name + "' has been closed");
        }
        participant.getPublisher().revert(element);
    }

    public void mutePublishedMedia(MutedMediaType muteType, String participantId)
            throws RoomException {
        log.debug("Request [MUTE_PUBLISHED] muteType={} ({})", muteType, participantId);
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        if (participant.isClosed()) {
            throw new RoomException(RoomException.Code.USER_CLOSED_ERROR_CODE, "Participant '" + name
                    + "' has been closed");
        }
        if (!participant.isStreaming()) {
            throw new RoomException(RoomException.Code.USER_NOT_STREAMING_ERROR_CODE, "Participant '" + name
                    + "' is not streaming media");
        }
        participant.mutePublishedMedia(muteType);
    }

    public void unmutePublishedMedia(String participantId) throws RoomException {
        log.debug("Request [UNMUTE_PUBLISHED] muteType={} ({})", participantId);
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        if (participant.isClosed()) {
            throw new RoomException(RoomException.Code.USER_CLOSED_ERROR_CODE, "Participant '" + name
                    + "' has been closed");
        }
        if (!participant.isStreaming()) {
            throw new RoomException(RoomException.Code.USER_NOT_STREAMING_ERROR_CODE, "Participant '" + name
                    + "' is not streaming media");
        }
        participant.unmutePublishedMedia();
    }

    public void muteSubscribedMedia(String remoteName, MutedMediaType muteType, String participantId)
            throws RoomException {
        log.debug("Request [MUTE_SUBSCRIBED] remoteParticipant={} muteType={} ({})", remoteName,
                muteType, participantId);
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        Room room = participant.getRoom();
        Participant senderParticipant = room.getParticipantByName(remoteName);
        if (senderParticipant == null) {
            log.warn("PARTICIPANT {}: Requesting to mute streaming from {} "
                    + "in room {} but user could not be found", name, remoteName, room.getName());
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "User " + remoteName
                    + " not found in room " + room.getName());
        }
        if (!senderParticipant.isStreaming()) {
            log.warn("PARTICIPANT {}: Requesting to mute streaming from {} "
                    + "in room {} but user is not streaming media", name, remoteName, room.getName());
            throw new RoomException(RoomException.Code.USER_NOT_STREAMING_ERROR_CODE, "User '" + remoteName
                    + " not streaming media in room '" + room.getName() + "'");
        }
        participant.muteSubscribedMedia(senderParticipant, muteType);
    }

    public void unmuteSubscribedMedia(String remoteName, String participantId) throws RoomException {
        log.debug("Request [UNMUTE_SUBSCRIBED] remoteParticipant={} ({})", remoteName, participantId);
        Participant participant = getParticipant(participantId);
        String name = participant.getName();
        Room room = participant.getRoom();
        Participant senderParticipant = room.getParticipantByName(remoteName);
        if (senderParticipant == null) {
            log.warn("PARTICIPANT {}: Requesting to unmute streaming from {} "
                    + "in room {} but user could not be found", name, remoteName, room.getName());
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "User " + remoteName
                    + " not found in room " + room.getName());
        }
        if (!senderParticipant.isStreaming()) {
            log.warn("PARTICIPANT {}: Requesting to unmute streaming from {} "
                    + "in room {} but user is not streaming media", name, remoteName, room.getName());
            throw new RoomException(RoomException.Code.USER_NOT_STREAMING_ERROR_CODE, "User '" + remoteName
                    + " not streaming media in room '" + room.getName() + "'");
        }
        participant.unmuteSubscribedMedia(senderParticipant);
    }

    public void addCaller(String userName, String participantId, String roomName) throws CallerAlreadyExistsException{
        RoomCustom room = rooms.get(roomName);
        new AddCaller(userName, participantId, room);
    }


    // ----------------- ADMIN (DIRECT or SERVER-SIDE) REQUESTS ------------
    /**
     * Closes all resources. This method has been annotated with the @PreDestroy directive
     * (javax.annotation package) so that it will be automatically called when the RoomManager
     * instance is container-managed. <br/>
     * <strong>Dev advice:</strong> Send notifications to all participants to inform that their room
     * has been forcibly closed.
     *
     * @see RoomManager#closeRoom(String)
     */
    @PreDestroy
    public void close() {
        closed = true;
        log.info("Closing all rooms");
        for (String roomName : rooms.keySet()) {
            try {
                closeRoom(roomName);
            } catch (Exception e) {
                log.warn("Error closing room '{}'", roomName, e);
            }
        }
    }


    public boolean isClosed() {
        return closed;
    }

    public boolean hasRoom(String roomName){
        for (String _roomName : rooms.keySet()) {
            if(_roomName.equals(roomName)){
                return true;
            }
        }
        return false;
    }
    public Set<String> getRooms() {
        return new HashSet<String>(rooms.keySet());
    }

    public Set<UserParticipant> getParticipants(String roomName) throws RoomException {
        Room room = rooms.get(roomName);

        if (room == null) {
            throw new RoomException(RoomException.Code.ROOM_NOT_FOUND_ERROR_CODE, "Room '" + roomName + "' not found");
        }

        Collection<Participant> participants = room.getParticipants();
        Set<UserParticipant> userParts = new HashSet<UserParticipant>();
        for (Participant p : participants) {
            if (!p.isClosed()) {
                userParts.add(new UserParticipant(p.getId(), p.getName(), p.isStreaming()));
            }
        }
        return userParts;
    }


    public Set<UserParticipant> getPublishers(String roomName) throws RoomException {
        Room r = rooms.get(roomName);
        if (r == null) {
            throw new RoomException(RoomException.Code.ROOM_NOT_FOUND_ERROR_CODE, "Room '" + roomName + "' not found");
        }
        Collection<Participant> participants = r.getParticipants();
        Set<UserParticipant> userParts = new HashSet<UserParticipant>();
        for (Participant p : participants) {
            if (!p.isClosed() && p.isStreaming()) {
                userParts.add(new UserParticipant(p.getId(), p.getName(), true));
            }
        }
        return userParts;
    }

    public Set<UserParticipant> getSubscribers(String roomName) throws RoomException {
        Room r = rooms.get(roomName);
        if (r == null) {
            throw new RoomException(RoomException.Code.ROOM_NOT_FOUND_ERROR_CODE, "Room '" + roomName + "' not found");
        }
        Collection<Participant> participants = r.getParticipants();
        Set<UserParticipant> userParts = new HashSet<UserParticipant>();
        for (Participant p : participants) {
            if (!p.isClosed() && p.isSubscribed()) {
                userParts.add(new UserParticipant(p.getId(), p.getName(), p.isStreaming()));
            }
        }
        return userParts;
    }


    public Set<UserParticipant> getPeerPublishers(String participantId) throws RoomException {
        Participant participant = getParticipant(participantId);
        if (participant == null) {
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "No participant with id '"
                    + participantId + "' was found");
        }
        Set<String> subscribedEndpoints = participant.getConnectedSubscribedEndpoints();
        Room room = participant.getRoom();
        Set<UserParticipant> userParts = new HashSet<UserParticipant>();
        for (String epName : subscribedEndpoints) {
            Participant p = room.getParticipantByName(epName);
            userParts.add(new UserParticipant(p.getId(), p.getName()));
        }
        return userParts;
    }

    public Set<UserParticipant> getPeerSubscribers(String participantId) throws RoomException {
        Participant participant = getParticipant(participantId);
        if (participant == null) {
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "No participant with id '"
                    + participantId + "' was found");
        }
        if (!participant.isStreaming()) {
            throw new RoomException(RoomException.Code.USER_NOT_STREAMING_ERROR_CODE, "Participant with id '"
                    + participantId + "' is not a publisher yet");
        }
        Set<UserParticipant> userParts = new HashSet<UserParticipant>();
        Room room = participant.getRoom();
        String endpointName = participant.getName();
        for (Participant p : room.getParticipants()) {
            if (p.equals(participant)) {
                continue;
            }
            Set<String> subscribedEndpoints = p.getConnectedSubscribedEndpoints();
            if (subscribedEndpoints.contains(endpointName)) {
                userParts.add(new UserParticipant(p.getId(), p.getName()));
            }
        }
        return userParts;
    }

    public boolean isPublisherStreaming(String participantId) throws RoomException {
        Participant participant = getParticipant(participantId);
        if (participant == null) {
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "No participant with id '"
                    + participantId + "' was found");
        }
        if (participant.isClosed()) {
            throw new RoomException(RoomException.Code.USER_CLOSED_ERROR_CODE, "Participant '" + participant.getName()
                    + "' has been closed");
        }
        return participant.isStreaming();
    }

    public void createRoom(KurentoClientSessionInfo kcSessionInfo) throws RoomException {
        String roomName = kcSessionInfo.getRoomName();

        RoomCustom room = rooms.get(kcSessionInfo);

        if (room != null) {
            throw new RoomException(RoomException.Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE, "Room '" + roomName + "' already exists");
        }

        KurentoClient kurentoClient = kcProvider.getKurentoClient(kcSessionInfo);

        room = new RoomCustom(roomName, kurentoClient, roomHandler, kcProvider.destroyWhenUnused());

        RoomCustom oldRoom = rooms.putIfAbsent(roomName, room);

        if (oldRoom != null) {
            log.warn("Room '{}' has just been created by another thread", roomName);
            return;
            // throw new CallerAlreadyExistsException(
            // Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE,
            // "Room '"
            // + roomName
            // + "' already exists (has just been created by another thread)");
        }
        String kcName = "[NAME NOT AVAILABLE]";
        if (kurentoClient.getServerManager() != null) {
            kcName = kurentoClient.getServerManager().getName();
        }
        log.warn("No room '{}' exists yet. Created one " + "using KurentoClient '{}'.", roomName,
                kcName);
    }

    public Set<UserParticipant> closeRoom(String roomName) throws RoomException {
        Room room = rooms.get(roomName);

        System.out.println("CLOSING ROOM '"+roomName+"'");

        if (room == null) {
            throw new RoomException(RoomException.Code.ROOM_NOT_FOUND_ERROR_CODE, "Room '" + roomName + "' not found");
        }
        if (room.isClosed()) {
            throw new RoomException(RoomException.Code.ROOM_CLOSED_ERROR_CODE, "Room '" + roomName + "' already closed");
        }

        Set<UserParticipant> participants = getParticipants(roomName);
        // copy the ids as they will be removed from the map
        Set<String> pids = new HashSet<String>(room.getParticipantIds());
        for (String pid : pids) {
            try {
                room.leave(pid);
            } catch (RoomException e) {
                log.warn("Error evicting participant with id '{}' from room '{}'", pid, roomName, e);
            }
        }
        room.close();
        rooms.remove(roomName);
        log.warn("Room '{}' removed and closed", roomName);
        return participants;
    }

    public MediaPipeline getPipeline(String participantId) throws RoomException {
        Participant participant = getParticipant(participantId);
        if (participant == null) {
            throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "No participant with id '"
                    + participantId + "' was found");
        }
        return participant.getPipeline();
    }

    public String getRoomName(String participantId) throws RoomException {
        Participant participant = getParticipant(participantId);
        return participant.getRoom().getName();
    }

    public String getParticipantName(String participantId) throws RoomException {
        Participant participant = getParticipant(participantId);
        return participant.getName();
    }

    public UserParticipant getParticipantInfo(String participantId) throws RoomException {
        Participant participant = getParticipant(participantId);
        return new UserParticipant(participantId, participant.getName());
    }

    // ------------------ HELPERS ------------------------------------------

    private Participant getParticipant(String pid) throws RoomException {
        for (Room r : rooms.values()) {
            if (!r.isClosed()) {
                if (r.getParticipantIds().contains(pid) && r.getParticipant(pid) != null) {
                    return r.getParticipant(pid);
                }
            }
        }
        throw new RoomException(RoomException.Code.USER_NOT_FOUND_ERROR_CODE, "No participant with id '" + pid
                + "' was found");
    }
}
