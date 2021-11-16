import {idiom, ng} from 'entcore';
import {distributionService, formService} from "../services";
import {Delegate, Form} from "../models";
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";
import {Delegates} from "../models/Delegate";

interface ViewModel {
    form: Form;
    delegates: Delegates;
    display: {
        date_ending: boolean
    }

    save() : Promise<void>;
    checkIntervalDates() : boolean;
    getImage() : void;
    getRgpdDescriptionIntro() : string;
    getRgpdDescriptionDelegates(delegate: Delegate) : string;
}


export const formPropController = ng.controller('FormPropController', ['$scope',
    function ($scope) {

        const vm: ViewModel = this;
        vm.form = new Form();
        vm.delegates = new Delegates();
        vm.display = {
            date_ending: false
        };

        const init = async () : Promise<void> => {
            vm.form = $scope.form;
            await vm.delegates.sync();
            vm.display.date_ending = !!vm.form.date_ending;
            vm.form.nb_responses = !!vm.form.id ? $scope.getDataIf200(await distributionService.count(vm.form.id)).count : 0;
            $scope.safeApply();
        };

        // Functions

        vm.save = async () : Promise<void> => {
            if (vm.form.title && vm.checkIntervalDates()) {
                let form = new Form();
                let data = $scope.getDataIf200(await formService.save(vm.form));
                console.log(data.id);
                form.setFromJson(data);
                $scope.redirectTo(`/form/${form.id}/edit`);
                $scope.safeApply();
            }
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

        vm.getRgpdDescriptionIntro = () : string => {
            let defaultGoal = "[" + idiom.translate('formulaire.prop.rgpd.goal') + "]";
            let params = [vm.form.rgpd_goal ? vm.form.rgpd_goal : defaultGoal, getEndValidityDate()];
            return $scope.getI18nWithParams('formulaire.prop.rgpd.description.intro', params);
        };

        vm.getRgpdDescriptionDelegates = (delegate) : string => {
            let params = [delegate.entity, delegate.mail, delegate.address, delegate.zipcode];
            return $scope.getI18nWithParams('formulaire.prop.rgpd.description.delegates', params);
        };

        const getEndValidityDate = () : string => {
            let today = new Date();
            today.setFullYear(today.getFullYear() + 1);
            return today.toLocaleDateString();
        };

        init();

        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_FORM_PROP, () => { init() });
    }]);