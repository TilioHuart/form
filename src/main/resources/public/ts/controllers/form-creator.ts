import {idiom, ng, notify} from 'entcore';
import {formService} from "../services";
import {Form} from "../models";

interface ViewModel {
    form: Form;

    save(): Promise<void>;
}


export const formCreatorController = ng.controller('FormCreatorController', ['$scope', 'FormService',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();

    const init = async (): Promise<void> => {
    };

    vm.save = async () : Promise<void> => {
        let response = await formService.create(vm.form);
        if (response.status == 200) {
            $scope.redirectTo(`/form/${response.data.id}`);
            $scope.safeApply();
        }
    };

    init();
}]);