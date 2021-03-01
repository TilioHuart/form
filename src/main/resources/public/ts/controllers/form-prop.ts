import {idiom, ng, notify} from 'entcore';
import {formService} from "../services";
import {Form} from "../models";
import {DateUtils} from "../utils/date";

interface ViewModel {
    form: Form;
    display: {
        date_ending: boolean;
    }

    save(): Promise<void>;
    checkIntervalDates(): boolean;
    getImage(): void;
    displayLastSave(): string;
}


export const formPropController = ng.controller('FormPropController', ['$scope', 'FormService',
    function ($scope) {

        const vm: ViewModel = this;
        vm.form = new Form();
        vm.display = {
            date_ending: false
        };

        const init = async (): Promise<void> => {
            vm.form = $scope.form;
            vm.display.date_ending = !!vm.form.date_ending;
        };

        // Functions

        vm.save = async (): Promise<void> => {
            let form = new Form();
            form.setFromJson($scope.getDataIf200(await formService.save(vm.form)));
            $scope.redirectTo(`/form/${form.id}`);
            $scope.safeApply();
        };

        vm.checkIntervalDates = () : boolean => {
            if (!!!vm.form.date_ending) {
                vm.form.date_ending = new Date(vm.form.date_opening);
                vm.form.date_ending.setFullYear(vm.form.date_ending.getFullYear() + 1);
            }
            return vm.form.date_ending > vm.form.date_opening;
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