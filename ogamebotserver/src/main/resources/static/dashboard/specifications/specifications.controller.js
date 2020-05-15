(function () {
    'use strict';

    angular
        .module('app')
        .controller('SpecificationController', SpecificationController);

    SpecificationController.$inject = ['UserService', '$rootScope','$mdSidenav','$state'];
    function SpecificationController(UserService, $rootScope,$mdSidenav,$state) {
        var vm = this;

        vm.init = function(){
            $rootScope.title = "Specifications";
        };
    }
})();