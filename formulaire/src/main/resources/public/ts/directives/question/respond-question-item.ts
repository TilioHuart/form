import {Directive, ng} from "entcore";
import {
    Distribution,
    Question, QuestionChoice,
    Response,
    ResponseFiles,
    Responses,
    Types
} from "../../models";
import {I18nUtils} from "@common/utils";

interface IViewModel {
    question: Question;
    responses: Responses;
    distribution: Distribution;
    files: Array<File>;
    Types: typeof Types;
    I18n: I18nUtils;
    mapChoiceResponseIndex: Map<QuestionChoice, number>;

    $onInit() : Promise<void>;
    getHtmlDescription(description: string) : string;
    $onChanges(changes: any): Promise<void>;
}

export const respondQuestionItem: Directive = ng.directive('respondQuestionItem', ['$sce', ($sce) => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            question: '<',
            responses: '=',
            distribution: '=',
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
                        <textarea ng-model="vm.responses.all[0].answer" i18n-placeholder="[[vm.question.placeholder || 'formulaire.question.type.SHORTANSWER']]" input-guard></textarea>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.LONGANSWER">
                        <editor ng-model="vm.responses.all[0].answer" input-guard></editor>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.SINGLEANSWER">
                        <select ng-model="vm.responses.all[0].choice_id" input-guard>
                            <option ng-value="">[[vm.I18n.translate('formulaire.options.select')]]</option>
                            <option ng-repeat="choice in vm.question.choices.all" ng-value="choice.id">[[choice.value]]</option>
                        </select>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.MULTIPLEANSWER">
                        <div ng-repeat="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label>
                                <input type="checkbox" ng-model="vm.responses.all[vm.mapChoiceResponseIndex.get(choice)].selected" input-guard>
                                <span>[[choice.value]]</span>
                            </label>
                        </div>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.DATE">
                        <date-picker ng-model="vm.responses.all[0].answer" input-guard></date-picker>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.TIME">
                        <input type="time" ng-model="vm.responses.all[0].answer" input-guard/>
                    </div>
                    <div ng-if="vm.question.question_type == vm.Types.FILE">
                        <formulaire-picker-file files="vm.files" multiple="true" input-guard></formulaire-picker-file>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.SINGLEANSWERRADIO">
                        <div ng-repeat ="choice in vm.question.choices.all | orderBy:['position', 'id']">
                            <label>
                                <input type="radio" ng-model="vm.responses.all[0].choice_id" ng-value="choice.id" input-guard>[[choice.value]]
                            </label>
                        </div>
                    </div>
                    <div ng-if ="vm.question.question_type == vm.Types.CURSOR">
                        <div class="formulaire-cursor-input-wrapper">
                            <div>
                                <label>[[vm.question.cursor_label_min_val]]</label> <!-- label minimum value (optional) -->
                            </div>
                            <div class="formulaire-cursor-input-range">
                                <div class="range-slider"
                                     style="--min:[[vm.question.cursor_min_val]];
                                            --max:[[vm.question.cursor_max_val]];
                                            --step:[[vm.question.cursor_step]];
                                            --value:[[vm.responses.all[0].answer]];">
                                    <!-- native cursor -->
                                    <input type="range" class="twelve" ng-model="vm.responses.all[0].answer"
                                           min="[[vm.question.cursor_min_val]]" max="[[vm.question.cursor_max_val]]" 
                                           step="[[vm.question.cursor_step]]">
                                   <!-- pin cursor -->
                                    <output class="pin">
                                        <div class="pin-content">[[vm.responses.all[0].answer]]</div>
                                    </output>
                                    <!-- progress bar -->
                                    <div class="filler"></div>
                                </div>
                                
                            
                                <!-- Display MIN and MAX -->
                                <div class="formulaire-cursor-input-range-values">
                                    <div>[[vm.question.cursor_min_val]]</div> <!-- minimum value -->
                                    <div>[[vm.question.cursor_max_val]]</div> <!-- maximum value -->
                                </div>
                            </div>
                            <div>
                                <label>[[vm.question.cursor_label_max_val]]</label> <!-- label maximum value (optional) -->
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                await initRespondQuestionItem();
            };

            vm.$onChanges = async (changes: any) : Promise<void> => {
                vm.question = changes.question.currentValue;
                await initRespondQuestionItem();
            };

            const initRespondQuestionItem = async () : Promise<void> => {
                if (vm.question.isTypeMultipleRep()) {
                    let existingResponses: Responses = new Responses();
                    if (vm.distribution) await existingResponses.syncMine(vm.question.id, vm.distribution.id);
                    vm.mapChoiceResponseIndex = new Map();
                    for (let choice of vm.question.choices.all) {
                        // Get potential existing response for this choice
                        let existingMatchingResponses: Response[] = existingResponses.all.filter((r:Response) => r.choice_id == choice.id);

                        // Get default response matching this choice and get its index in list
                        let matchingResponses: Response[] = vm.responses.all.filter((r:Response) => r.choice_id == choice.id);
                        if (matchingResponses.length != 1) console.error("Be careful, 'vm.responses' has been badly implemented !!");
                        let matchingIndex = vm.responses.all.indexOf(matchingResponses[0]);

                        // If there was an existing response we use it to replace the default one
                        if (existingMatchingResponses.length == 1) {
                            vm.responses.all[matchingIndex] = existingMatchingResponses[0];
                            vm.responses.all[matchingIndex].selected = true;
                        }

                        vm.mapChoiceResponseIndex.set(choice, matchingIndex);
                    }
                }
                else if (vm.distribution) {
                    let responses: Responses = new Responses();
                    await responses.syncMine(vm.question.id, vm.distribution.id);
                    if (responses.all.length > 0) vm.responses.all[0] = responses.all[0];
                    if (!vm.responses.all[0].question_id) vm.responses.all[0].question_id = vm.question.id;
                    if (!vm.responses.all[0].distribution_id) vm.responses.all[0].distribution_id = vm.distribution.id;
                    console.log("response for ", vm.question, " : ", vm.responses.all[0]);
                }

                if (vm.question.question_type === Types.TIME && typeof vm.responses.all[0].answer == "string") {
                    vm.responses.all[0].answer = new Date("January 01 1970 " + vm.responses.all[0].answer);
                }

                if (vm.question.question_type === Types.FILE) {
                    vm.files = new Array<File>();
                    if (vm.responses.all[0].id) {
                        let responseFiles: ResponseFiles = new ResponseFiles();
                        await responseFiles.sync(vm.responses.all[0].id);
                        for (let repFile of responseFiles.all) {
                            if (repFile.id)  {
                                let file: File = new File([repFile.id], repFile.filename);
                                vm.files.push(file);
                            }
                        }
                    }
                }

                if (vm.question.question_type === Types.CURSOR && typeof vm.responses.all[0].answer == "string") {
                    vm.responses.all[0].answer = Number(vm.responses.all[0].answer);
                }

                $scope.$apply();
            };
        },
        link: function ($scope) {
            const vm: IViewModel = $scope.vm;
            vm.Types = Types;
            vm.I18n = I18nUtils;

            vm.getHtmlDescription = (description: string) : string => {
                return !!description ? $sce.trustAsHtml(description) : null;
            }
        }
    };
}]);
