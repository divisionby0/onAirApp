package dev.div0.jsonPrcUserControlOperations;


import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.commons.PropertiesManager;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DetectClientUriCorrectOperation {

    private final String CLIENTS_URLS = PropertiesManager.getProperty("clients.urls");

    public DetectClientUriCorrectOperation(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager) throws IOException, InterruptedException, ExecutionException {
        System.out.println("CLIENTS_URLS = "+CLIENTS_URLS);
        String clientServerUri = JsonRpcUserControlUtil.getStringParam(request, "clientUri");
        roomManager.isClientUriCorrect(clientServerUri, participantRequest, CLIENTS_URLS);
    }
}
