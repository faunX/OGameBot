(function () {
    'use strict';

    angular
        .module('app')
        .controller('DashboardController', DashboardController);

    DashboardController.$inject = ['$rootScope','$timeout','$http',"$interval","$state","$stomp","$log","ChatService"];
    function DashboardController($rootScope,$timeout,$http,$interval,$state,$stomp,$log,ChatService) {
        var vm = this;
        vm.dataLoaded = false;

        vm.init = function () {
            $rootScope.title = "Stuff";
            var i = 0;
            load();

            // $interval(function() {
            //     loadImages();
            //     // console.log(i+ +);
            // },5000);
            // setInterval(loadImages(),1000);
        };
        
        function load() {
            ChatService.receive().then(null, null, function(payload) {
                var message = payload;
                if(vm.image == message)
                    return;
                vm.image = message;
                // messageList.append("<li>" + message + "</li>");
                var img = document.getElementById("100");//[0]
                if(!img) return;
                img = img[0];
                if(!img) return;
                img.setAttribute("data-ng-src", "data:image/png;base64,"+vm.image);
            });

        }

        function loadImages() {
            // var userToken = '';
            vm.dataLoaded = false;
            var loginreq = {
                method: 'GET',
                url: '/getImage',
                // responseType: 'arraybuffer',
                transformResponse: []//,
                // headers:{
                //     'udusername': self.username ,
                //     'udpassword': self.password
                // }
            };
            $http(loginreq)
                .then(
                    function (response) {
                        // console.log($rootScope.tests);
                        vm.dataLoaded = true;
                        if(vm.image == response.data)
                            return;
                        vm.image = response.data;
                        var img = document.getElementById("100")[0];
                        if(!img) return;
                        img.setAttribute("data-ng-src", "data:image/png;base64,"+vm.image);

                        //"data:image/png;base64,{{vm.image}}";

                        // $timeout(function () {
                        //     $rootScope.$apply(function () {
                        //             var iframe = document.getElementById("100");
                        //             // document.body.appendChild(iframe);
                        //
                        //             var frameDoc = iframe.document;
                        //             if(iframe.contentWindow)
                        //                 frameDoc = iframe.contentWindow.document; // IE
                        //             // Write into iframe
                        //             frameDoc.open();
                        //             frameDoc.clear();
                        //             frameDoc.writeln(vm.image);
                        //             frameDoc.close();
                            //     }
                            // );
                            // $state.reload();
                        // });
                    },
                    function (result) {
                        console.log("Failed to Login" +JSON.stringify(result));
                        // UIHelper.showToast("Invalid Credentials");
                    }
                );
        }
        vm.image = '';
    }
})();