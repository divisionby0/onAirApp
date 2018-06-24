package dev.div0.utils;

import com.google.gson.JsonObject;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.room.rpc.ParticipantSession;

import java.util.Map;

public class JsonRpcUserControlUtil{

    public static String getStringParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing");
        }
        return request.getParams().get(key).getAsString();
    }

    public static int getIntParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing");
        }
        return request.getParams().get(key).getAsInt();
    }

    public static boolean getBooleanParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing");
        }
        return request.getParams().get(key).getAsBoolean();
    }

    public static ParticipantSession getParticipantSession(Transaction transaction) {
        Session session = transaction.getSession();
        ParticipantSession participantSession = (ParticipantSession) session.getAttributes().get(
                ParticipantSession.SESSION_KEY);
        if (participantSession == null) {
            participantSession = new ParticipantSession();
            session.getAttributes().put(ParticipantSession.SESSION_KEY, participantSession);
        }
        return participantSession;
    }


    public static void printTransaction(Transaction transaction){
        System.out.println("Print transaction ...");
        Session session = transaction.getSession();
        System.out.println("Session");
        Map<String, Object> sessionAttributes = session.getAttributes();

        Object registerInfo = session.getRegisterInfo();

        System.out.println("attributes total "+sessionAttributes.size());

        for (Map.Entry<String, Object> entry : sessionAttributes.entrySet())
        {
            System.out.println("key:"+entry.getKey() + "/" + entry.getValue());
        }
        System.out.println("register info "+registerInfo);
    }

    public static void printRequest(Request<JsonObject> request){
        System.out.println("print request");
        System.out.println(request.toString());
    }


}
