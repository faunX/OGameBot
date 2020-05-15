(function () {
    'use strict';

    function DialogController($scope, $mdDialog) {
        $scope.hide = function() {
            $mdDialog.hide();
        };
        $scope.cancel = function() {
            $mdDialog.cancel();
        };
        $scope.answer = function(answer) {
            $mdDialog.hide(answer);
        };
    }
    angular.module('app').controller('ListBottomSheetCtrl', function($scope, $mdBottomSheet) {
        $scope.items = [
            { name: 'Share', icon: 'share' },
            { name: 'Upload', icon: 'upload' },
            { name: 'Copy', icon: 'copy' },
            { name: 'Print this page', icon: 'print' },
        ];

        $scope.listItemClick = function($index) {
            var clickedItem = $scope.items[$index];
            $mdBottomSheet.hide(clickedItem);
        };
    });

    angular
        .module('app')
        .controller('MainController', MainController);

    MainController.$inject = ['UserService', '$rootScope','$mdSidenav','$state','$mdDialog','$mdBottomSheet'];
    function MainController(UserService, $rootScope,$mdSidenav,$state,$mdDialog,$mdBottomSheet) {
        var vm = this;

        vm.user = null;
        vm.allUsers = [];
        vm.show = false;
        vm.deleteUser = deleteUser;

        initController();

        $rootScope.toggleSidenav = function(menuId) {
            $mdSidenav(menuId).toggle();
        };

        $rootScope.showAdd = function ($event) {
            $mdDialog.show({
                controller: DialogController,
                template: '<md-dialog aria-label="Mango (Fruit)"> <md-content class="md-padding"> <form name="userForm"> <div layout layout-sm="column"> <md-input-container flex> <label>First Name</label> <input ng-model="user.firstName" placeholder="Placeholder text"> </md-input-container> <md-input-container flex> <label>Last Name</label> <input ng-model="theMax"> </md-input-container> </div> <md-input-container flex> <label>Address</label> <input ng-model="user.address"> </md-input-container> <div layout layout-sm="column"> <md-input-container flex> <label>City</label> <input ng-model="user.city"> </md-input-container> <md-input-container flex> <label>State</label> <input ng-model="user.state"> </md-input-container> <md-input-container flex> <label>Postal Code</label> <input ng-model="user.postalCode"> </md-input-container> </div> <md-input-container flex> <label>Biography</label> <textarea ng-model="user.biography" columns="1" md-maxlength="150"></textarea> </md-input-container> </form> </md-content> <md-dialog-actions layout="row"> <span flex></span> <md-button ng-click="answer(\'not useful\')"> Cancel </md-button> <md-button ng-click="answer(\'useful\')" class="md-primary"> Save </md-button> </div></md-dialog-actions>',
                targetEvent: event
            })
                .then(function(answer) {
                    $rootScope.alert = 'You said the information was "' + answer + '".';
                }, function() {
                    $rootScope.alert = 'You cancelled the dialog.';
                });
        };

        $rootScope.alert = '';
        vm.showListBottomSheet = function($event) {
            $rootScope.alert = '';
            $mdBottomSheet.show({
                template: '<md-bottom-sheet class="md-list md-has-header"> <md-subheader>Settings</md-subheader> <md-list> <md-item ng-repeat="item in items"><md-item-content md-ink-ripple flex class="inset"> <a flex aria-label="{{item.name}}" ng-click="listItemClick($index)"> <span class="md-inline-list-icon-label">{{ item.name }}</span> </a></md-item-content> </md-item> </md-list></md-bottom-sheet>',
                controller: 'ListBottomSheetCtrl',
                targetEvent: $event
            }).then(function (clickedItem) {
                $rootScope.alert = clickedItem.name + ' clicked!';
            });
        };

        function initController() {
            loadCurrentUser();
            loadAllUsers();

            $rootScope.title = "Dashboard";
        }

        function loadCurrentUser() {
            UserService.GetByUsername(window.localStorage.getItem("currentUser").username)
                .then(function (user) {
                    vm.user = user;
                });
        }

        function loadAllUsers() {
            UserService.GetAll()
                .then(function (users) {
                    vm.allUsers = users;
                });
        }

        function deleteUser(id) {
            UserService.Delete(id)
                .then(function () {
                    loadAllUsers();
                });
        }

        vm.goTo = function(url){
            $state.go(url,{}, {reload:true});
            $mdSidenav('left').toggle();
        };

        $rootScope.menu = [
            {
                link : 'main.dashboard',
                title: 'Dashboard',
                icon: 'dashboard'
            },
            {
                link : 'main.tests',
                title: 'Tests',//in here i'll have your tests and all public tests (tests are public by default)
                icon: 'bug_report'//also have the ability to make a new test by either downloading one or writing one
            },
            {
                link : 'main.specifications',
                title: 'Specifications',
                icon: 'assessment'
            }
        ];
        $rootScope.admin = [
            {
                link : 'showListBottomSheet($event)',
                title: 'Settings',
                icon: 'settings'
            }
        ];

        $rootScope.tabs = [
            {
                id : 'tab1',
                name : 'Favorites'
            }
        ]
    }
})();
