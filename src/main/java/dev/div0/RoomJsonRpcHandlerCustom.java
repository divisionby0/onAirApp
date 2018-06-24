package dev.div0;

import com.google.gson.JsonObject;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.RoomJsonRpcHandler;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.internal.ProtocolElements;
import org.kurento.room.rpc.JsonRpcNotificationService;

import org.kurento.room.rpc.ParticipantSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

public class RoomJsonRpcHandlerCustom  extends DefaultJsonRpcHandler<JsonObject> {
    private static final Logger log = LoggerFactory.getLogger(RoomJsonRpcHandler.class);

    private static final String HANDLER_THREAD_NAME = "handler";

    private JsonRpcUserControlCustom userControl;

    private JsonRpcNotificationServiceCustom notificationService;

    @Autowired
    public RoomJsonRpcHandlerCustom(JsonRpcUserControlCustom userControl, JsonRpcNotificationServiceCustom notificationService) {
        this.userControl = userControl;
        this.notificationService = notificationService;
    }

    @Override
    public List<String> allowedOrigins() {

        return Arrays.asList("*");
    }

    @Override
    public final void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

        String sessionId = null;
        try {
            sessionId = transaction.getSession().getSessionId();
        } catch (Throwable e) {
            log.warn("Error getting session id from transaction {}", transaction, e);
            throw e;
        }

        updateThreadName(HANDLER_THREAD_NAME + "_" + sessionId);

        //log.debug("Session #{} - request: {}", sessionId, request);

        notificationService.addTransaction(transaction, request);

        ParticipantRequest participantRequest = new ParticipantRequest(sessionId, Integer.toString(request.getId()));

        transaction.startAsync();

        switch (request.getMethod()) {
            case "isClientUriCorrect" :
                userControl.isClientUriCorrect(transaction, request, participantRequest);
                break;
            case "roomHasPublisher" :
                userControl.isRoomHasPublisher(transaction, request, participantRequest);
                break;
            case ProtocolElements.JOINROOM_METHOD :
                userControl.joinRoom(transaction, request, participantRequest);
                break;
            case ProtocolElements.PUBLISHVIDEO_METHOD :
                userControl.publishVideo(transaction, request, participantRequest);
                break;
            case ProtocolElements.UNPUBLISHVIDEO_METHOD :
                userControl.unpublishVideo(transaction, request, participantRequest);
                break;
            case ProtocolElements.RECEIVEVIDEO_METHOD :
                userControl.receiveVideoFrom(transaction, request, participantRequest);
                break;
            case ProtocolElements.UNSUBSCRIBEFROMVIDEO_METHOD :
                userControl.unsubscribeFromVideo(transaction, request, participantRequest);
                break;
            case ProtocolElements.ONICECANDIDATE_METHOD :
                userControl.onIceCandidate(transaction, request, participantRequest);
                break;
            case ProtocolElements.LEAVEROOM_METHOD :
                userControl.leaveRoom(transaction, request, participantRequest);
                break;
            case ProtocolElements.SENDMESSAGE_ROOM_METHOD :
                userControl.sendMessage(transaction, request, participantRequest);
                break;
            case ProtocolElements.CUSTOMREQUEST_METHOD :
                userControl.customRequest(transaction, request, participantRequest);
                break;
            case "roomExists" :
                userControl.isRoomExists(transaction, request, participantRequest);
                break;
            case "incomingCall" :
                userControl.incomingCall(transaction, request, participantRequest);
                break;
            default :
                log.error("Unrecognized request {}", request);
                break;
        }

        updateThreadName(HANDLER_THREAD_NAME);
    }

    @Override
    public final void afterConnectionClosed(Session session, String status) throws Exception {
        ParticipantSession ps = null;
        if (session.getAttributes().containsKey(ParticipantSession.SESSION_KEY)) {
            ps = (ParticipantSession) session.getAttributes().get(ParticipantSession.SESSION_KEY);
        }
        String sid = session.getSessionId();
        log.debug("CONN_CLOSED: sessionId={}, participant in session: {}", sid, ps);
        ParticipantRequest preq = new ParticipantRequest(sid, null);
        updateThreadName(sid + "|wsclosed");

        userControl.leaveRoom(null, null, preq);

        updateThreadName(HANDLER_THREAD_NAME);
    }

    @Override
    public void handleTransportError(Session session, Throwable exception) throws Exception {
        if(session!=null){
            System.out.println("Transport error for session id {} "+ session.getSessionId() +"  exception="+ exception);
        }
        else{
            System.out.println("Transport error for session id {} NULL_SESSION   exception="+ exception);
        }

    }

    private void updateThreadName(String name) {

        Thread.currentThread().setName("user:" + name);
    }
}
