package dev.div0.utils;

public class MessageData {
    private String userName;
    private String roomName;
    private String message;

    public MessageData(String userName, String roomName, String message){
        this.userName = userName;
        this.roomName = roomName;
        this.message = message;
    }

    public String getUserName() {
        return userName;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getMessage() {
        return message;
    }
}
