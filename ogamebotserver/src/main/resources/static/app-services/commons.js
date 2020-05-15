angular.module('app').factory('UIHelper',
    ['$mdDialog', '$mdMedia', '$mdToast', function($mdDialog, $mdMedia, $mdToast) {
        var UIHelper = {};
        UIHelper.showPopup = function(controller, url, stateParams){
            var useFullScreen = $mdMedia('max-width: 960px');
            stateParams.fullScreen = useFullScreen;
            return $mdDialog.show({
                controller: controller,
                controllerAs: 'ctrl',
                templateUrl: 'app/scripts/' + url,
                locals : {
                    $stateParams: stateParams
                },
                parent: angular.element(document.body),
                clickOutsideToClose:true
            })
        };
        UIHelper.showToast = function(title,content){
            return $mdDialog.show(
                $mdDialog.alert()
                    .parent(angular.element(document.querySelector('#main')))
                    .clickOutsideToClose(true)
                    .title(title)
                    .textContent(content)
                    .ariaLabel('Alert Dialog')
                    .ok('OK')
            );
        };
        return UIHelper;
    }]

)/**
 * Created by jarndt on 4/17/17.
 */
