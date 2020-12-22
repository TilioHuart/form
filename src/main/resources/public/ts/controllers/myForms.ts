import {idiom, model, ng, template, toasts} from 'entcore';
import {Form} from '../models/Form';
import {formService} from '../services';

interface ViewModel {
}


export const myFormsController = ng.controller('MyFormsController', ['$scope', 'FormService',
    function ($scope) {

    $scope.lang = idiom;
    $scope.template = template;
    const vm: ViewModel = this;

    const init = async (): Promise<void> => {

    };

    init();
}]);