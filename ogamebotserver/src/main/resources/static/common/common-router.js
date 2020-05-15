angular.module('app').config(['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.otherwise('/login');

    $stateProvider
        .state('main', {
            abstract: true,
            url: '/m',
            templateUrl:'common/main.html',
            controller: 'MainController',
            controllerAs: 'vm'
        })
        .state('main.dashboard',{
            url: '/dashboard',
            templateUrl: 'dashboard/dashboard.view.html',
            controller: 'DashboardController',
            controllerAs: 'vm'
        })
        .state('main.tests',{
            url: '/tests',
            templateUrl: 'dashboard/tests/tests.view.html',
            controller: 'TestController',
            controllerAs: 'vm'
        })
        .state('main.specifications',{
            url: '/specifications',
            templateUrl: 'dashboard/specifications/specifications.view.html',
            controller: 'SpecificationController',
            controllerAs: 'vm'
        })
        .state('login',{
            url: '/login',
            templateUrl: 'login/login.view.html',
            controller: 'LoginController',
            controllerAs: 'vm'
        })
        .state('register',{
            url: '/register',
            templateUrl: 'register/register.view.html',
            controller: 'RegisterController',
            controllerAs: 'vm'
        })
}]);