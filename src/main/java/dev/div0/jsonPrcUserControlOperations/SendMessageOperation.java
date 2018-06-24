package dev.div0.jsonPrcUserControlOperations;

import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcMessageDataParser;
import dev.div0.utils.MessageData;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMessageOperation {

    private static final Logger log = LoggerFactory.getLogger(SendMessageOperation.class);

    public SendMessageOperation(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager){
        MessageData messageData = JsonRpcMessageDataParser.parse(request);
        log.debug("Message from {} in room {}: '{}'", messageData.getUserName(), messageData.getRoomName(), messageData.getMessage());

        roomManager.sendMessage(messageData.getMessage(), messageData.getUserName(), messageData.getRoomName(), participantRequest);
    }
}
