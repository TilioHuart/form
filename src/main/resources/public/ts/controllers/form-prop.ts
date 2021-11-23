import {ng} from 'entcore';
import {distributionService, formService} from "../services";
import {Form} from "../models";
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";
import {Delegates} from "../models";
import {Folder} from "../models/Folder";

interface ViewModel {
    form: Form;
    folder: Folder;
    delegates: Delegates;
    display: {
        date_ending: boolean
    }

    save() : Promise<void>;
    checkIntervalDates() : boolean;
    getImage() : void;
}


export const formPropController = ng.controller('FormPropController', ['$scope',
    function ($scope) {

        const vm: ViewModel = this;
        vm.form = new Form();
        vm.folder = new Folder();
        vm.delegates = new Delegates();
        vm.display = {
            date_ending: false
        };

        const init = async () : Promise<void> => {
            vm.form = $scope.form;
            vm.folder = $scope.folder;
            await vm.delegates.sync();
            vm.display.date_ending = !!vm.form.date_ending;
            vm.form.nb_responses = vm.form.id ? $scope.getDataIf200(await distributionService.count(vm.form.id)).count : 0;
            vm.form.folder_id = vm.folder.id;
            $scope.safeApply();
        };

        // Functions

        vm.save = async () : Promise<void> => {
            if (vm.form.title && vm.checkIntervalDates()) {
                vm.form = $scope.getDataIf200(await formService.save(vm.form));
                $scope.redirectTo(`/form/${vm.form.id}/edit`);
                $scope.safeApply();
            }
        };

        vm.checkIntervalDates = () : boolean => {
            if (vm.display.date_ending) {
                if (!vm.form.date_ending) {
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

        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_FORM_PROP, () => { init() });
    }]);