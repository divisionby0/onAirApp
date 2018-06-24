package dev.div0.caller;

import dev.div0.CallerAlreadyExistsException;
import dev.div0.RoomCustom;

public class AddCaller{

    public AddCaller(String userName, String participantId, RoomCustom room) throws CallerAlreadyExistsException{
        room.addCaller(userName, participantId);
    }
}
