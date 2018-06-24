package dev.div0;

import org.kurento.client.KurentoClient;
import org.kurento.room.api.RoomHandler;
import org.kurento.room.internal.Participant;
import org.kurento.room.internal.Room;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class RoomCustom extends Room{
    private final ConcurrentMap<String, Participant> callers = new ConcurrentHashMap<String, Participant>();
    private String ownerName;

    public RoomCustom(String roomName, KurentoClient kurentoClient, RoomHandler roomHandler, boolean destroyKurentoClient) {
        super(roomName, kurentoClient, roomHandler, destroyKurentoClient);
        System.out.println("callers:"+callers);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {

        this.ownerName = ownerName;
    }

    public void addCaller(String name, String participantId) throws CallerAlreadyExistsException{
        System.out.println("RoomCustom adding caller name="+name);
        System.out.println("participantId="+participantId);

        boolean callerExists = callers.get(participantId)!=null;

        System.out.println("callerExists "+callerExists);

        if(callerExists){
            throw new CallerAlreadyExistsException("CallerAlreadyExistsAtRoom");
        }
        else{
            Participant caller = getParticipant(participantId);

            System.out.println("caller:"+caller);
            System.out.println("participantId:"+participantId);


            callers.put(participantId, caller);

            System.out.println("callers:"+callers);
        }
    }

    public Participant getCaller(String id){
        return callers.get(id);
    }

    public Participant removeCaller(String id){
        return callers.remove(id);
    }

}
