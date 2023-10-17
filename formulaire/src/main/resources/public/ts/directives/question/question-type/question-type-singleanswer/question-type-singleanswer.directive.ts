import {Directive, ng} from "entcore";
import {Form, FormElement, FormElements, Question, QuestionChoice} from "@common/models";
import {I18nUtils} from "@common/utils";
import {Direction} from "@common/core/enums";
import {IScope} from "angular";
import {RootsConst} from "../../../../core/constants/roots.const";

interface IQuestionTypeSingleanswerProps {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    isRadio: boolean;
    form: Form;
}

interface IViewModel extends ng.IController, IQuestionTypeSingleanswerProps {
    followingFormElement: FormElement;
    i18n: I18nUtils;
    direction: typeof Direction;
    selectedChoiceIndex: number;

    deleteChoice(index: number): Promise<void>;
    displayImageSelect(index: number): void;
    deleteImageSelect(index: number): void;
    filterNextElements(formElement: FormElement): boolean;
    onSelectOption(choice: QuestionChoice): void;
}

interface IQuestionTypeSingleanswerScope extends IScope, IQuestionTypeSingleanswerProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    isRadio: boolean;
    form: Form;
    followingFormElement: FormElement;
    i18n: I18nUtils;
    direction: typeof Direction;
    selectedChoiceIndex: number;

    constructor(private $scope: IQuestionTypeSingleanswerScope, private $sce: ng.ISCEService) {
        this.i18n = I18nUtils;
        this.direction = Direction;
        this.selectedChoiceIndex = -1;
    }

    $onInit = async () : Promise<void> => {
        for (let choice of this.question.choices.all) {
            choice.next_form_element = choice.getNextFormElement(this.formElements, this.question);
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

    displayImageSelect(index: number): void {
        this.selectedChoiceIndex = index;
    }

    deleteImageSelect = (index: number): void => {
        this.selectedChoiceIndex = -1;
        const choice = this.question.choices.all[index];
        choice.image = null;
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
            isRadio: '<',
            form: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IQuestionTypeSingleanswerScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const questionTypeSingleanswer: Directive = ng.directive('questionTypeSingleanswer', directive);