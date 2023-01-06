import {Directive, ng} from "entcore";
import {
    Distribution,
    Question,
    QuestionChoices,
    Response,
    ResponseFiles,
    Responses,
    Types
} from "../../models";
import {responseService} from "../../services";
import {Mix} from "entcore-toolkit";
import {I18nUtils} from "@common/utils";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";

interface IViewModel {
    question: Question;
    response: Response;
    distribution: Distribution;
    selectedIndex: boolean[];
    responsesChoices: Responses;
    files: any;
    Types: typeof Types;
    I18n: I18nUtils;

    $onInit() : Promise<void>;
    getHtmlDescription(description: string) : string;
}

export const respondQuestionItem: Directive = ng.directive('respondQuestionItem', ['$sce', ($sce) => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '=',
            response: '=',
            distribution: '=',
            selectedIndex: '=',
            responsesChoices: '=',
            files: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
            <div class="question" guard-root>
                <div class="question-title">
                    <h4>[[vm.question.title]]<span ng-if="vm.question.mandatory" style="color:red;margin-left:10px">*</span></h4>
                </div>
                <div class="question-main">
                    <div ng-if="vm.question.question_type == vm.Types.FREETEXT">
                        <div ng-if="vm.question.statement" data-ng-bind-html="vm.getHtmlDescription(vm.question.statement)"></div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SHORTANSWER">
                        <textarea ng-model="vm.response.answer" i18n-placeholder="[[vm.question.placeholder]]" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.LONGANSWER">
                        <editor ng-model="vm.response.answer" input-guard></editor>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWER">
                        <select ng-model="vm.response.choice_id" input-guard>
                            <option ng-value="">[[vm.I18n.translate('formulaire.options.select')]]</option>
                            <option ng-repeat="choice in vm.question.choices.all" ng-value="choice.id">[[choice.value]]</option>
                        </select>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER">
                        <div ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label for="check-[[choice.id]]">
                                <input type="checkbox" id="check-[[choice.id]]" ng-model="vm.selectedIndex[$index]" input-guard>
                                <span>[[choice.value]]</span>
                            </label>
                        </div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.DATE">
                        <date-picker ng-model="vm.response.answer" input-guard></date-picker>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.TIME">
                        <input type="time" ng-model="vm.response.answer" input-guard/>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.FILE">
                        <formulaire-picker-file files="vm.files" multiple="true" input-guard></formulaire-picker-file>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                        <div ng-repeat ="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label>
                                <input type="radio" ng-model="vm.response.choice_id" ng-value="[[choice.id]]" input-guard>[[choice.value]]
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                vm.files = [];
                vm.responsesChoices = new Responses();
                vm.question.choices = new QuestionChoices();

                if (vm.question.question_type === Types.MULTIPLEANSWER
                    || vm.question.question_type === Types.SINGLEANSWER
                    || vm.question.question_type === Types.SINGLEANSWERRADIO) {
                    await vm.question.choices.sync(vm.question.id);
                }
                if (vm.question.question_type === Types.MULTIPLEANSWER && vm.distribution) {
                    vm.selectedIndex = [];
                    await vm.responsesChoices.syncMine(vm.question.id, vm.distribution.id);
                    vm.selectedIndex = new Array<boolean>(vm.question.choices.all.length);
                    for (let i = 0; i < vm.selectedIndex.length; i++) {
                        let check: boolean = false;
                        let j: number = 0;
                        while (!check && j < vm.responsesChoices.all.length) {
                            check = vm.question.choices.all[i].id === vm.responsesChoices.all[j].choice_id;
                            j++;
                        }
                        vm.selectedIndex[i] = check;
                    }
                }
                else if (vm.distribution) {
                    vm.response = new Response();
                    let responses: any = await responseService.listMineByDistribution(vm.question.id, vm.distribution.id);
                    if (responses.length > 0) {
                        vm.response = Mix.castAs(Response, responses[0]);
                    }
                    if (!vm.response.question_id) { vm.response.question_id = vm.question.id; }
                    if (!vm.response.distribution_id) { vm.response.distribution_id = vm.distribution.id; }
                    $scope.$apply();
                }
                if (vm.question.question_type === Types.TIME) { formatTime() }
                if (vm.question.question_type === Types.FILE) {
                    vm.files = [];
                    if (vm.response.id) {
                        let responseFiles: ResponseFiles = new ResponseFiles();
                        await responseFiles.sync(vm.response.id);
                        for (let repFile of responseFiles.all) {
                            if (repFile.id)  {
                                let file: File = new File([repFile.id], repFile.filename);
                                vm.files.push(file);
                            }
                        }
                    }
                }
                $scope.$apply();
            };

            const formatTime = () : void => {
                if (vm.response.answer) {
                    vm.response.answer = new Date("January 01 1970 " + vm.response.answer);
                }
            };
        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.I18n = I18nUtils;

            vm.getHtmlDescription = (description: string) : string => {
                return !!description ? $sce.trustAsHtml(description) : null;
            }

            $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.REFRESH_QUESTION, () => { vm.$onInit(); });
        }
    };
}]);
