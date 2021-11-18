import {Directive, idiom, ng} from "entcore";
import {Delegate, Delegates, Form} from "../models";
import {i18nUtils} from "../utils";

interface IViewModel {
    form: Form;
    delegates: Delegates;
    i18nUtils: i18nUtils;

    $onInit() : Promise<void>;
    getRgpdDescriptionIntro() : string;
    getRgpdDescriptionDelegates(delegate: Delegate) : string;
}

export const rgpd: Directive = ng.directive('rgpd', () => {

    return {
        restrict: 'E',
        scope: {
            form: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="rgpd">
                <div class="bottom-spacing-three" bind-html="vm.getRgpdDescriptionIntro()"></div>
                <div ng-repeat="delegate in vm.delegates.all" bind-html="vm.getRgpdDescriptionDelegates(delegate)"></div>
            </div>
        `,

        controller: function($scope) {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                vm.delegates = new Delegates();
                await vm.delegates.sync();
                $scope.$apply();
            };
        },
        link: function ($scope, $element) {
            const vm: IViewModel = $scope.vm;

            vm.getRgpdDescriptionIntro = () : string => {
                let defaultGoal = "[" + idiom.translate('formulaire.prop.rgpd.goal') + "]";
                let params = [vm.form.rgpd_goal ? vm.form.rgpd_goal : defaultGoal, getEndValidityDate()];
                return i18nUtils.getWithParams('formulaire.prop.rgpd.description.intro', params);
            };

            vm.getRgpdDescriptionDelegates = (delegate) : string => {
                let params = [delegate.entity, delegate.mail, delegate.address, delegate.zipcode];
                return i18nUtils.getWithParams('formulaire.prop.rgpd.description.delegates', params);
            };

            const getEndValidityDate = () : string => {
                let today = new Date();
                today.setFullYear(today.getFullYear() + 1);
                return today.toLocaleDateString();
            };
        }
    };
});
