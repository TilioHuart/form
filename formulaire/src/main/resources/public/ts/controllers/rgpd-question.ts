import {ng} from "entcore";
import {Delegates, Distributions, DistributionStatus, Form} from "../models";
import {FORMULAIRE_BROADCAST_EVENT, FORMULAIRE_EMIT_EVENT} from "@common/core/enums";
import {distributionService} from "../services";

interface ViewModel {
    form: Form;
    distributions: Distributions;
    delegates: Delegates;

    $onInit() : Promise<void>;
    quit() : void;
    startForm() : Promise<void>;
}

export const rgpdQuestionController = ng.controller('RgpdQuestionController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.form = new Form();
    vm.distributions = new Distributions();
    vm.delegates = new Delegates();

    vm.$onInit = async () : Promise<void> => {
        vm.form = $scope.form;
        await vm.distributions.syncByFormAndResponder(vm.form.id);
        await vm.delegates.sync();
        $scope.safeApply();
    };

    vm.quit = () : void => {
        $scope.redirectTo(`/list/responses`);
    };

    vm.startForm = async () : Promise<void> => {
        if (!vm.form.multiple) {
            let distrib = vm.distributions.all[0];
            if (distrib.status == DistributionStatus.TO_DO) {
                $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, {path: `/form/${vm.form.id}/${distrib.id}`});
            }
            else {
                $scope.redirectTo(`/form/${vm.form.id}/${distrib.id}/questions/recap`);
            }
        }
        else {
            let distrib = vm.distributions.all.filter(d => d.status == DistributionStatus.TO_DO)[0];
            distrib = distrib ? distrib : await distributionService.add(vm.distributions.all[0].id);
            $scope.$emit(FORMULAIRE_EMIT_EVENT.REDIRECT, {path: `/form/${vm.form.id}/${distrib.id}`});
        }
        $scope.safeApply();
    };

    $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_RGPD_QUESTION, () => { vm.$onInit() });
}]);