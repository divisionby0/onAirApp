package dev.div0.jsonPrcUserControlOperations;

import com.google.gson.JsonObject;
import dev.div0.NotificationRoomManagerCustom;
import dev.div0.utils.JsonRpcUserControlUtil;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.internal.ProtocolElements;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DetectRoomExistanceOperation {
    public DetectRoomExistanceOperation(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest, NotificationRoomManagerCustom roomManager) throws IOException, InterruptedException, ExecutionException {
        System.out.println("isRoomExists request");
        String roomName = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.JOINROOM_ROOM_PARAM);
        System.out.println("roomName = "+roomName);

        boolean roomExists = roomManager.getRooms().contains(roomName);

        System.out.println("roomExists = "+roomExists);

        roomManager.sendRoomExistsResponse(participantRequest, roomExists, roomName);
    }
}
