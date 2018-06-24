/*
 * @author Raquel Díaz González
 */

function log(message){
    console.log(message);
    var logText = document.getElementById("logContainer").innerHTML;
    logText+="\n"+message;
    document.getElementById("logContainer").innerHTML  = logText;
}

kurento_room.service('ServiceRoom', function () {

    log("Im ServiceRoom");
    log("kurento="+kurento);
    log("roomName="+roomName);
    log("userName="+userName);
    log("localStream="+localStream);
    var kurento;
    var roomName;
    var userName;
    var localStream;

    this.getKurento = function () {
        return kurento;
    };

    this.getRoomName = function () {
        return roomName;
    };

    this.setKurento = function (value) {
        kurento = value;
    };

    this.setRoomName = function (value) {
        roomName = value;
    };

    this.getLocalStream = function () {
        return localStream;
    };

    this.setLocalStream = function (value) {
        localStream = value;
    };

    this.getUserName = function () {
        return userName;
    };

    this.setUserName = function (value) {
        userName = value;
    };
});
