(function () {
    'use strict';

    angular
        .module('app')
        .controller('RegisterController', RegisterController);

    RegisterController.$inject = ['UserService', '$location', '$rootScope','UIHelper',"$http"];
    function RegisterController(UserService, $location, $rootScope,UIHelper,$http) {
        var vm = this;

        vm.register = register;

        function register() {
            vm.dataLoading = true;

            var loginreq = {
                method:'POST',
                url:'/register',
                transformResponse: [],
                headers:{
                    'username': vm.user.username ,
                    'password': vm.user.password,
                    'firstName' : vm.user.firstName,
                    'lastName' : vm.user.lastName
                }
            };
            $http(loginreq)
                .then(
                    function(response){
                        vm.dataLoading = false;
                        UIHelper.showToast('Registration successful', response.message);
                        $location.path('/login');
                    },
                    function(result) {
                        // console.log("Failed to Login" +JSON.stringify(result));
                        UIHelper.showToast("Invalid Credentials", result.message);
                    }
                );
        }
    }

})();