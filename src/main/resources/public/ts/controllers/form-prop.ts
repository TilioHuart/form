import {idiom, ng} from 'entcore';
import {formService} from "../services";
import {Form} from "../models";
import {DateUtils} from "../utils/date";

interface ViewModel {
    form: Form;

    save(): Promise<void>;

    getImage(): void;

    displayLastSave(): string;
}


export const formPropController = ng.controller('FormPropController', ['$scope', 'FormService',
    function ($scope) {

        const vm: ViewModel = this;
        vm.form = new Form();

        const init = async (): Promise<void> => {
            vm.form = $scope.form;
        };

        // Functions

        vm.save = async (): Promise<void> => {
            let form = $scope.getDataIf200(await formService.save(vm.form));
            $scope.redirectTo(`/form/${form.id}`);
            $scope.safeApply();
        };

        vm.getImage = async (): Promise<void> => {
            // TODO Fix vm.form.setInfoImage() function not found
            if (vm.form.picture) {
                // await vm.form.setInfoImage();
                // window.setTimeout(function() {
                //     if(!vm.form.infoImg.compatible) {
                //         notify.error(idiom.translate('formulaire.image.incompatible'));
                //     }
                // }, 2000)
            }
            $scope.safeApply();
        };

        vm.displayLastSave = (): string => {
            let localDateTime = DateUtils.localise(vm.form.date_modification);
            let date = DateUtils.format(localDateTime, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
            let time = DateUtils.format(localDateTime, DateUtils.FORMAT["HOUR-MINUTES"]);
            return date + idiom.translate('formulaire.at') + time;
        };

        init();
    }]);