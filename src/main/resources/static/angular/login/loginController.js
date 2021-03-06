/*
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Raquel Díaz González
 */


kurento_room.controller('loginController', function ($scope, $http, ServiceParticipant, $window, ServiceRoom, LxNotificationService) {

    var ver = "0.0.3";
    console.log("Im login controller "+ver);
	var options;

    $http.get('/getAllRooms').
            success(function (data, status, headers, config) {
                console.log(JSON.stringify(data));
                $scope.listRooms = data;
            }).
            error(function (data, status, headers, config) {
            });

    $http.get('/getClientConfig').
             success(function (data, status, headers, config) {
            	console.log(JSON.stringify(data));
            	$scope.clientConfig = data;
             }).
             error(function (data, status, headers, config) {
             });
    
    $http.get('/getUpdateSpeakerInterval').
	    success(function (data, status, headers, config) {
	        $scope.updateSpeakerInterval = data
	    }).
	    error(function (data, status, headers, config) {
	});

    $http.get('/getThresholdSpeaker').
    	success(function (data, status, headers, config) {
    		$scope.thresholdSpeaker = data
		}).
		error(function (data, status, headers, config) {
	});
    
    $scope.register = function (room) {
    	
    	if (!room)
    		ServiceParticipant.showError($window, LxNotificationService, {
    			error: {
    				message:"Username and room fields are both required"
    			}
    		});
    	
        $scope.userName = room.userName;
        $scope.roomName = room.roomName;

        //var wsUri = 'wss://' + location.host + '/room';
        var wsUri = 'ws://localhost:8443/room';
        console.log("loginController wsUri="+wsUri);

        //show loopback stream from server
        var displayPublished = $scope.clientConfig.loopbackRemote || false;
        //also show local stream when display my remote
        var mirrorLocal = $scope.clientConfig.loopbackAndLocal || false;
        
        var kurento = KurentoRoom(wsUri, function (error, kurento) {

            if (error)
                return console.log(error);

            //TODO token should be generated by the server or a 3rd-party component  
            //kurento.setRpcParams({token : "securityToken"});

            room = kurento.Room({
                room: $scope.roomName,
                user: $scope.userName,
                updateSpeakerInterval: $scope.updateSpeakerInterval,
                thresholdSpeaker: $scope.thresholdSpeaker 
            });

            var localStream = kurento.Stream(room, {
                audio: true,
                video: true,
                data: false
            });

            localStream.addEventListener("access-accepted", function () {
                room.addEventListener("room-connected", function (roomEvent) {
                	var streams = roomEvent.streams;
                	if (displayPublished ) {
                		localStream.subscribeToMyRemote();
                	}
                	localStream.publish();
                    ServiceRoom.setLocalStream(localStream.getWebRtcPeer());
                    for (var i = 0; i < streams.length; i++) {
                        ServiceParticipant.addParticipant(streams[i]);
                    }
                });

                room.addEventListener("stream-published", function (streamEvent) {
                	 ServiceParticipant.addLocalParticipant(localStream);
                	 if (mirrorLocal && localStream.displayMyRemote()) {
                		 var localVideo = kurento.Stream(room, {
                             video: true,
                             id: "localStream"
                         });
                		 localVideo.mirrorLocalStream(localStream.getWrStream());
                		 ServiceParticipant.addLocalMirror(localVideo);
                	 }
                });
                
                room.addEventListener("stream-added", function (streamEvent) {
                    ServiceParticipant.addParticipant(streamEvent.stream);
                });

                room.addEventListener("stream-removed", function (streamEvent) {
                    ServiceParticipant.removeParticipantByStream(streamEvent.stream);
                });

                room.addEventListener("newMessage", function (msg) {
                    ServiceParticipant.showMessage(msg.room, msg.user, msg.message);
                });
                room.addEventListener("onIsOwnerStateChanged", function (msg) {
                    console.log("room onIsOwnerStateChanged ");
                });
                room.addEventListener("isOwner", function (msg) {
                    console.log("room isOwner ");
                });

                room.addEventListener("error-room", function (error) {
                    ServiceParticipant.showError($window, LxNotificationService, error);
                });

                room.addEventListener("error-media", function (msg) {
                    ServiceParticipant.alertMediaError($window, LxNotificationService, msg.error, function (answer) {
                    	console.warn("Leave room because of error: " + answer);
                    	if (answer) {
                    		kurento.close(true);
                    	}
                    });
                });
                
                room.addEventListener("room-closed", function (msg) {
                	if (msg.room !== $scope.roomName) {
                		console.error("Closed room name doesn't match this room's name", 
                				msg.room, $scope.roomName);
                	} else {
                		kurento.close(true);
                		ServiceParticipant.forceClose($window, LxNotificationService, 'Room '
                			+ msg.room + ' has been forcibly closed from server');
                	}
                });
                
                room.addEventListener("lost-connection", function(msg) {
                    kurento.close(true);
                    ServiceParticipant.forceClose($window, LxNotificationService,
                      'Lost connection with room "' + msg.room +
                      '". Please try reloading the webpage...');
                  });
                
                room.addEventListener("stream-stopped-speaking", function (participantId) {
                    ServiceParticipant.streamStoppedSpeaking(participantId);
                 });

                 room.addEventListener("stream-speaking", function (participantId) {
                    ServiceParticipant.streamSpeaking(participantId);
                 });

                 room.addEventListener("update-main-speaker", function (participantId) {
                     ServiceParticipant.updateMainSpeaker(participantId);
                  });

                room.connect();
            });

            localStream.addEventListener("access-denied", function () {
            	ServiceParticipant.showError($window, LxNotificationService, {
            		error : {
            			message : "Access not granted to camera and microphone"
            				}
            	});
            });
            localStream.init();
        });

        //save kurento & roomName & userName in service
        ServiceRoom.setKurento(kurento);
        ServiceRoom.setRoomName($scope.roomName);
        ServiceRoom.setUserName($scope.userName);

        //redirect to call
        $window.location.href = '#/call';
    };
    $scope.clear = function () {
        $scope.room = "";
        $scope.userName = "";
        $scope.roomName = "";
    };
});


