package dev.div0;

import com.google.gson.JsonObject;
import dev.div0.jsonPrcUserControlOperations.*;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.commons.PropertiesManager;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.internal.ProtocolElements;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class JsonRpcUserControlCustom{
    private static final Logger log = LoggerFactory.getLogger(JsonRpcUserControl.class);
    protected NotificationRoomManagerCustom roomManager;

    @Autowired
    public JsonRpcUserControlCustom(NotificationRoomManagerCustom roomManager) {
        log.info("Im JsonRpcUserControlCustom roomManager="+roomManager);
        this.roomManager = roomManager;
    }

    public void isClientUriCorrect(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) throws IOException, InterruptedException, ExecutionException{
        new DetectClientUriCorrectOperation(transaction, request, participantRequest, roomManager);
    }

    public void isRoomHasPublisher(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) throws IOException, InterruptedException, ExecutionException{
        new DetectRoomHasPublisherOperation(transaction, request, participantRequest, roomManager);
    }

    public void isRoomExists(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) throws IOException, InterruptedException, ExecutionException{
        new DetectRoomExistanceOperation(transaction, request, participantRequest, roomManager);
    }

    public void incomingCall(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) throws IOException, InterruptedException, ExecutionException{
        new IncomingCallOperation(transaction, request, participantRequest, roomManager);
    }

    public void joinRoom(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) throws IOException, InterruptedException, ExecutionException {
        new JoinRoomOperation(transaction, request, participantRequest, roomManager);
    }

    public void publishVideo(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) {
        new PublishVideoOperation(transaction, request, participantRequest, roomManager);
    }

    public void unpublishVideo(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) {
        roomManager.unpublishMedia(participantRequest);
    }

    public void receiveVideoFrom(final Transaction transaction, final Request<JsonObject> request, ParticipantRequest participantRequest) {
        new ReceiveVideoOperation(transaction, request, participantRequest, roomManager);
    }

    public void unsubscribeFromVideo(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) {
        new UnsubscribeVideoOperation(transaction, request, participantRequest, roomManager);
    }

    public void leaveRoomAfterConnClosed(String sessionId) {
        new LeaveRoomAfterConnClosedOperation(sessionId, roomManager);
    }

    public void leaveRoom(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) {
        new LeaveRoomOperation(transaction, request, participantRequest, roomManager);
    }

    public void onIceCandidate(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) {
        new OnIceCandidateOperation(transaction, request, participantRequest, roomManager);
    }

    public void sendMessage(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) {
        new SendMessageOperation(transaction, request, participantRequest, roomManager);
    }

    public void customRequest(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) {
        throw new RuntimeException("Unsupported method");
    }
}