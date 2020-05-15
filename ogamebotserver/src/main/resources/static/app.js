var app = angular.module('app',
    [
        'ngMaterial',
        'ui.router',
        'ngMdIcons',
        'ngCookies',
        'ngSanitize',
        'ngStomp'
    ])
    .config(['$mdThemingProvider', function($mdThemingProvider) {
        $mdThemingProvider.theme('input', 'default')
            .primaryPalette('grey')

    // var lightGreenMap = $mdThemingProvider.extendPalette('light-green', {
    //     '200': 'aecc5a'
    // });
    // $mdThemingProvider.definePalette('lighter-green', lightGreenMap);
    // $mdThemingProvider.theme('default')
    //     .primaryPalette('light-green')
    //     .accentPalette('lighter-green', {
    //         'default': '200'
    //     })
}]);

