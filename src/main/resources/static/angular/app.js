/*
 * @author Raquel Díaz González
 */
var ver = "0.0.4";
var kurento_room = angular.module('kurento_room', ['ngRoute', 'FBAngular', 'lumx']);
console.log("Version: "+ver);
kurento_room.config(function ($routeProvider) {

    $routeProvider
            .when('/', {
                templateUrl: 'angular/login/login.html',
                controller: 'loginController'
            })
            .when('/login', {
                templateUrl: 'angular/login/login.html',
                controller: 'loginController'
            })
            .when('/call', {
                templateUrl: 'angular/call/call.html',
                controller: 'callController'
            });
//            .otherwise({
//                templateUrl: 'error.html',
//                controller: 'MainController',
//            });
});
$( document ).ready(function() {
    console.log( "ready!" );
    $("#appInfoContainer").text("app ver: "+ver+"   Browser: "+webrtcDetectedBrowser);
});



