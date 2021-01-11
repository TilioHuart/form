import {ng} from 'entcore';

interface ViewModel {
}


export const formEditorController = ng.controller('FormEditorController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;

    const init = async (): Promise<void> => {
    };

    init();
}]);