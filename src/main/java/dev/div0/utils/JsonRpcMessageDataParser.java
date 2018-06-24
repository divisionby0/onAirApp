package dev.div0.utils;

import com.google.gson.JsonObject;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.internal.ProtocolElements;


public class JsonRpcMessageDataParser {
    public static MessageData parse(Request<JsonObject> request){
        String userName = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.SENDMESSAGE_USER_PARAM);
        String roomName = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.SENDMESSAGE_ROOM_PARAM);
        String message = JsonRpcUserControlUtil.getStringParam(request, ProtocolElements.SENDMESSAGE_MESSAGE_PARAM);

        return new MessageData(userName, roomName, message);
    }
}
