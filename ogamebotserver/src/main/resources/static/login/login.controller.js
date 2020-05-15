(function () {
    'use strict';

    angular
        .module('app')
        .controller('LoginController', LoginController);

    LoginController.$inject = ['$state', 'AuthenticationService', 'UIHelper','$http'];
    function LoginController($state, AuthenticationService, UIHelper,$http) {
        var vm = this;

        vm.login = login;

        (function initController() {
            // reset login status
            AuthenticationService.ClearCredentials();
        })();

        function login() {
            vm.dataLoading = true;
            var loginreq = {
                method:'POST',
                url:'/login',
                transformResponse: [],
                headers:{
                    'username': vm.username ,
                    'password': vm.password
                }
            };
            $http(loginreq)
                .then(
                    function(response){
                        vm.dataLoading = false;
                        // UIHelper.showToast('Registration successful', response.message);
                        AuthenticationService.SetCredentials(vm.username, vm.password);
                        $state.go('main.dashboard',{}, {reload: true});
                    },
                    function(result) {
                        // console.log("Failed to Login" +JSON.stringify(result));
                        UIHelper.showToast("Invalid Credentials","Please Enter Correct Credentials");
                        vm.dataLoading = false;
                    }
                );
        };
    }

})();