import {Directive, ng} from "entcore";
import {FormElement, FormElements, Question, QuestionChoice} from "@common/models";
import {I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {RootsConst} from "../../../../core/constants/roots.const";
import {IScope} from "angular";

interface IQuestionTypeSingleanswerProps {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    isRadio: boolean;
}

interface IQuestionTypeSingleanswerRadioScope extends IScope, IQuestionTypeSingleanswerProps{
    vm: IViewModel;
}

interface IViewModel extends ng.IController, IQuestionTypeSingleanswerProps {
    followingFormElement: FormElement;
    i18n: I18nUtils;
    direction: typeof Direction;

    deleteChoice(index: number): Promise<void>;
    filterNextElements(formElement: FormElement): boolean;
    onSelectOption(choice: QuestionChoice): void;
}

class Controller implements IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    isRadio: boolean;
    followingFormElement: FormElement;
    i18n: I18nUtils;
    direction: typeof Direction;

    constructor(private $scope: IQuestionTypeSingleanswerRadioScope, private $sce: ng.ISCEService) {
        this.i18n = I18nUtils;
        this.direction = Direction;
    }

    $onInit = async () : Promise<void> => {
        for (let choice of this.question.choices.all) {
            choice.setNextFormElement(this.formElements, this.question);
        }
        this.followingFormElement = this.question.getFollowingFormElement(this.formElements);
    }

    $onDestroy = async () : Promise<void> => {}

    deleteChoice = async (index: number) : Promise<void> => {
        await this.question.deleteChoice(index);
    }

    filterNextElements = (formElement: FormElement) : boolean => {
        let position: number = this.question.getPosition(this.formElements);
        return formElement.position > position && this.followingFormElement && formElement.id != this.followingFormElement.id;
    }

    onSelectOption = (choice: QuestionChoice) : void => {
        choice.next_form_element_id = choice.next_form_element ? choice.next_form_element.id : null;
        choice.next_form_element_type = choice.next_form_element ? choice.next_form_element.form_element_type : null;
        let followingFormElement: FormElement = this.question.getFollowingFormElement(this.formElements);
        choice.is_next_form_element_default = choice.next_form_element ?
            choice.next_form_element.equals(followingFormElement) :
            followingFormElement == null;
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}question/question-type/question-type-singleanswer/question-type-singleanswer.html`,
        transclude: true,
        scope: {
            question: '=',
            hasFormResponses: '=',
            formElements: '<',
            isRadio: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeSingleanswerRadioScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeSingleanswer: Directive = ng.directive('questionTypeSingleanswer', directive);