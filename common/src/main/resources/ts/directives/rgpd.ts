import {idiom, ng} from "entcore";
import {Delegate, Delegates, Form} from "../models";
import {I18nUtils} from "../utils";
import {IScope} from "angular";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";

interface IRgpdProps {
    form: Form;
    delegates: Delegates;
}

interface IViewModel extends ng.IController, IRgpdProps {
    init(): Promise<void>;
    getHtmlDescription(description: string): string;
    getRgpdDescriptionIntro(): string;
    getRgpdDescriptionDelegates(delegate: Delegate): string;
    getEndValidityDate(): string;
}

interface IRgpdScope extends IScope, IRgpdProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    form: Form;
    delegates: Delegates;

    constructor(private $scope: IRgpdScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {
        await this.init();
    }

    $onDestroy = async (): Promise<void> => {}

    init = async (): Promise<void> => {
        this.delegates = new Delegates();
        await this.delegates.sync();
        this.$scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { this.init(); });
        this.$scope.$apply();
    }

    getHtmlDescription = (description: string): string => {
        return !!description ? this.$sce.trustAsHtml(description): null;
    }

    getRgpdDescriptionIntro = () : string => {
        let defaultGoal: string = "[" + idiom.translate('formulaire.prop.rgpd.goal') + "]";
        let params: string[] = [this.form.rgpd_goal ? this.form.rgpd_goal : defaultGoal, this.getEndValidityDate()];
        return I18nUtils.getWithParams('formulaire.prop.rgpd.description.intro', params);
    }

    getRgpdDescriptionDelegates = (delegate: Delegate) : string => {
        let params: string[] = [delegate.entity, delegate.mail, delegate.address, delegate.zipcode, delegate.city];
        for (let i = 0; i < params.length; i++) {
            if (params[i] == null) {
                params[i] = "";
            }
        }
        let delegateDescription: string = I18nUtils.getWithParams('formulaire.prop.rgpd.description.delegates', params);
        return this.getHtmlDescription(delegateDescription);
    }

    getEndValidityDate = () : string => {
        let today: Date = new Date();
        today.setMonth(today.getMonth() + this.form.rgpd_lifetime);
        return today.toLocaleDateString();
    }
}

function directive() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            response: '=',
            selectedIndexes: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="rgpd">
                <div class="bottom-spacing-three" data-ng-bind-html="vm.getHtmlDescription(vm.getRgpdDescriptionIntro())"></div>
                <div ng-repeat="delegate in vm.delegates.all" data-ng-bind-html="vm.getRgpdDescriptionDelegates(delegate)"></div>
            </div>
        `,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IRgpdScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const rgpd = ng.directive('rgpd', directive);