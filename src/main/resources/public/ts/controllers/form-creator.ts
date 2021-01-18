import {idiom, ng, notify} from 'entcore';
import {formService} from "../services";
import {Form} from "../models";

interface ViewModel {
    form: Form;

    save(): Promise<void>;
    getImage(): void;
}


export const formCreatorController = ng.controller('FormCreatorController', ['$scope', 'FormService',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();

    const init = async (): Promise<void> => {
    };

    // Functions

    vm.save = async () : Promise<void> => {
        let response = await formService.create(vm.form);
        if (response.status == 200) {
            $scope.redirectTo(`/form/${response.data.id}`);
            $scope.safeApply();
        }
    };

    vm.getImage = function () {
        if (vm.form.picture) {
            // TODO : cf Moodle (+ back)
            // vm.form.setInfoImg();
            // $timeout(() =>
            //         $scope.imgCompatibleMoodle = $scope.course.infoImg.compatibleMoodle
            //     , 1000)
        }
        $scope.safeApply();
    };

    init();
}]);