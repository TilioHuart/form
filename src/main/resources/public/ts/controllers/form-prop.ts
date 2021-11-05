import {ng} from 'entcore';
import {distributionService, formService} from "../services";
import {Form} from "../models";
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";

interface ViewModel {
    form: Form;
    display: {
        date_ending: boolean
    }

    save() : Promise<void>;
    checkIntervalDates() : boolean;
    getImage() : void;
}


export const formPropController = ng.controller('FormPropController', ['$scope', 'FormService',
    function ($scope) {

        const vm: ViewModel = this;
        vm.form = new Form();
        vm.display = {
            date_ending: false
        };

        const init = async () : Promise<void> => {
            vm.form = $scope.form;
            vm.display.date_ending = !!vm.form.date_ending;
            vm.form.nb_responses = !!vm.form.id ? $scope.getDataIf200(await distributionService.count(vm.form.id)).count : 0;
            $scope.safeApply();
        };

        // Functions

        vm.save = async () : Promise<void> => {
            let form = new Form();
            form.setFromJson($scope.getDataIf200(await formService.save(vm.form)));
            $scope.redirectTo(`/form/${form.id}/edit`);
            $scope.safeApply();
        };

        vm.checkIntervalDates = () : boolean => {
            if (vm.display.date_ending) {
                if (!!!vm.form.date_ending) {
                    vm.form.date_ending = new Date(vm.form.date_opening);
                    vm.form.date_ending.setFullYear(vm.form.date_ending.getFullYear() + 1);
                }
                return vm.form.date_ending > vm.form.date_opening;
            }
            else {
                vm.form.date_ending = null;
                return true;
            }
        };

        vm.getImage = async () : Promise<void> => {
            if (vm.form.picture) {
                await vm.form.setInfoImage();
                // window.setTimeout(function() {
                //     if(!vm.form.infoImg.compatible) {
                //         notify.error(idiom.translate('formulaire.image.incompatible'));
                //     }
                // }, 2000)
            }
            $scope.safeApply();
        };

        init();

        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_CONTROLLER, () => { init() });
    }]);