(function () {
    'use strict';
    function DialogController($scope, $mdDialog) {
        var ctrl = this;

        $scope.hide = function() {
            $mdDialog.hide();
        };
        $scope.cancel = function() {
            $mdDialog.cancel();
        };
        $scope.answer = function(answer) {
            $mdDialog.hide(answer);
        };
        $scope.newTest = function () {
            // console.log("new Test");
            $mdDialog.hide("new_test");
        };
        $scope.importTest = function () {
            // console.log("importTest Test");
            $mdDialog.hide("import_test");
        }
    }
    angular
        .module('app')
        .controller('TestController', TestController);

    TestController.$inject = ['UIHelper', '$rootScope','$mdDialog','$timeout','$http'];
    function TestController(UIHelper, $rootScope,$mdDialog,$timeout,$http) {
        var vm = this;
        vm.dataLoaded = false;
        vm.resultLoading = false;
        vm.saveLoading = false;
        vm.deleteLoading = false;


        function getTests() {
            if(!vm.dataLoaded)
                return {};
            return $rootScope.tests;
        }

        vm.init = function(){
            $rootScope.title = "Tests";
            vm.textArea = "";
            loadTests();

            $rootScope.showAdd = function ($event) {
                $mdDialog.show({
                    controller: DialogController,
                    parent: angular.element(document.querySelector('#add')).parent(),
                    template:
                    '<div class="menu-panel" md-whiteframe="4">' +
                    '  <div class="menu-content" flex layout="column">' +
                    '      <button class="md-button" ng-click="newTest()">' +
                    '        <span>New Test</span>' +
                    '      </button>' +
                    '      <input class="ng-hide" id="input-file-id" multiple type="file" />' +
                    '      <label for="input-file-id" class="md-button">Import Test</label>' +
                    '    <md-divider></md-divider>' +
                    '    <div class="menu-item">' +
                    '      <button class="md-button" ng-click="cancel()">' +
                    '        <span>Close Menu</span>' +
                    '      </button>' +
                    '    </div>' +
                    '  </div>' +
                    '</div>',
                    targetEvent: event,
                    clickOutsideToClose: true
                })
                    .then(function(answer) {
                        // console.log('You said the information was "' + answer + '".');
                        var newTest = "new_test";
                        var importTest = "import_test";
                        if(answer === newTest)
                            return vm.newTest();
                        if(answer === importTest)
                            return vm.importTest();

                    }, function() {
                        // console.log('You cancelled the dialog.');
                    });
            };
        };
        vm.newTest = function(){
            // console.log("new test");
            getTests().push({ name: 'NEW_TEST_'+getDate()+".xml", content: '<root>\n</root>', active: false });
        };
        vm.importTest = function(){
            console.log("import test");
        };

        function getDate() {
            return new Date().toJSON();
        }

        function loadTests() {
            // var userToken = '';
            var loginreq = {
                method:'GET',
                url:'/gettests',
                transformResponse: [],
                // headers:{
                //     'udusername': self.username ,
                //     'udpassword': self.password
                // }
            };
            $http(loginreq)
                .then(
                    function(response){
                        // AppGlobalService.setToken(response.data);
                        // $state.go('main.mainScreen',{}, {reload: true});
                        // Fetch.load(false, 'getapihost', function(response){
                        //     $scope.$emit('loggedIn', response);
                        // });
                        $rootScope.tests = JSON.parse(response.data);
                        // console.log($rootScope.tests);
                        vm.dataLoaded = true;

                        $rootScope.tests[0].active = true;
                        $rootScope.activeTest = $rootScope.tests[0];
                        $timeout(function() {
                            $rootScope.$apply();
                        });
                        // vm.init();
                    },
                    function(result) {
                        // console.log("Failed to Login" +JSON.stringify(result));
                        UIHelper.showToast("Invalid Credentials");
                    }
                );

            // $rootScope.tests = [
            //     { name: 'Janet Perkins', content: '1', active: false },
            //     { name: 'Mary Johnson', content: '2', active: false },
            //     { name: 'Peter Carlsson', content: '3', active: false }
            // ];
        }

        vm.save = function () {
            if(!vm.editorDataChanged)
                return;
            $rootScope.activeTest.content = $rootScope._editor.getValue();

            vm.saveLoading = true;
            var loginreq = {
                method:'GET',
                url:'/savetest',
                transformResponse: [],
                headers:{
                    'name' : $rootScope.activeTest.name,
                    'content' : encodeURI($rootScope.activeTest.content)
                }
            };
            $http(loginreq)
                .then(
                    function(response){
                        vm.saveLoading = false;
                    },
                    function(result) {
                        // console.log("Failed to Login" +JSON.stringify(result));
                        UIHelper.showToast("Can't save: "+JSON.stringify(result));
                    }
                );
        };

        vm.goToTest = function (name, $event) {
            vm.save();
            var tests = getTests();
            for(var i = 0; i<tests.length; i++)
                if(tests[i].name === name) {
                    $rootScope.activeTest = getTests()[i];
                    getTests()[i].active = true;
                }else
                    getTests()[i].active = false;
            $rootScope._editor.setValue($rootScope.activeTest.content);
        };
        $rootScope.aceLoaded = function (_editor) {
            _editor.setOptions({
                enableBasicAutocompletion: true,
                enableSnippets: true,
                enableLiveAutocompletion: false
            });
            //TODO autocompletion
            var testCompleter = {
                getCompletions: function (editor, session, pos, prefix, callback) {
                    if(prefix.length === 0){callback(null,[]); return}
                    var wordList = [
                        // {"word":"ill","score":300},
                        // {"word":"il","score":300},
                        // {"word":"ele","score":300},
                        // {"word":"ille","score":300},
                        // {"word":"wil","score":300},
                        // {"word":"wilh","score":300},
                        // {"word":"till","score":300}
                    ];
                    callback(null, wordList.map(function(ea) {
                        return {name: ea.word, value: ea.word, score: ea.score, meta: "rhyme"}
                    }));
                }
            };
            _editor.completers.push(testCompleter);
            _editor.commands.addCommand({
                name: 'save',
                bindKey: {win: 'Ctrl-s',mac:'Ctrl-s'},
                exec: function(editor){vm.save()}
            });
            _editor.commands.addCommand({
                name: 'run',
                bindKey: {win: 'Ctrl-r',mac:'Ctrl-r'},
                exec: function(editor){vm.runTest()}
            });

            _editor.$blockScrolling = Infinity;
            $rootScope._editor = _editor;
            $rootScope._session = _editor.getSession();
            $rootScope._renderer = _editor.renderer;
            _editor.getSession().on('change',function(e){
               vm.editorDataChanged = true;
            });
            if(vm.dataLoaded)
                $rootScope._editor.setValue($rootScope.activeTest.content);
        };




        vm.rename = function (name,$event,showRename) {
            if(!$event || ($event && $event.which === 13)) {
                $rootScope.activeTest.name = name;
                if($event && $event.which === 13)
                    showRename = !showRename;
         }
         return showRename;
        };
        vm.delete = function (name, $event) {
            var confirm = $mdDialog.confirm()
                .parent(angular.element(document.querySelector('#main')))
                .clickOutsideToClose(true)
                .title("Delete Confirmation")
                .textContent("Are you sure you want to delete "+name)
                .ariaLabel('Alert Dialog')
                .targetEvent($event)
                .ok('YES')
                .cancel("NO");
            $mdDialog.show(confirm).then(function() {
                deleteTest(name);
            }, function() {
                // $scope.status = 'You decided to keep your record.';
            });
        };
        function deleteTest(name) {
            var tests = getTests();
            var index;
            for(var i = 0; i<tests.length; i++)
                if(tests[i].name === name)
                    index = i;

            vm.deleteLoading = true;
            var loginreq = {
                method:'GET',
                url:'/deletetest',
                transformResponse: [],
                headers:{
                    // 'udusername': self.username ,
                    'name': $rootScope.activeTest.name
                }
            };
            $http(loginreq)
                .then(
                    function(response){
                        vm.deleteLoading = false;
                    },
                    function(result) {
                        // console.log("Failed to Login" +JSON.stringify(result));
                        UIHelper.showToast("Can't delete Tests: "+JSON.stringify(result));
                    }
                );

            getTests().splice(index,1);
            getTests()[0].active = true;
            $rootScope.activeTest = getTests()[0];
            if(!$rootScope.activeTest)
                $rootScope._editor.setValue();
            else
                $rootScope._editor.setValue($rootScope.activeTest.content);
        }
        vm.getTests = function () {
            return getTests();
        };
        vm.runTest = function($event){
            if(vm.resultLoading || vm.saveLoading)
                return;
            if(vm.editorDataChanged)
                vm.save();
            
            vm.resultLoading = true;
            var loginreq = {
                method:'GET',
                url:'/runtest',
                transformResponse: [],
                headers:{
                    // 'udusername': self.username ,
                    'name': $rootScope.activeTest.name
                }
            };
            $http(loginreq)
                .then(
                    function(response){
                        vm.resultLoading = false;
                        // console.log(JSON.parse(response.data));
                        vm.textArea = JSON.parse(response.data).testContent;
                        $timeout(function() {
                            $rootScope.$apply();
                        });
                    },
                    function(result) {
                        // console.log("Failed to Login" +JSON.stringify(result));
                        UIHelper.showToast("Can't run Tests: "+result);
                    }
                );
        }
    }
})();