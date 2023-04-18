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
    i18n: I18nUtils;
    direction: typeof Direction;

    deleteChoice(index: number): Promise<void>;
    getPosition(): number;
    getNextElement(position?: number): FormElement;
    filterNextElements(formElement: FormElement): boolean;
    onSelectOption(choice: QuestionChoice): void;
}

class Controller implements IViewModel {
    question: Question;
    hasFormResponses: boolean;
    formElements: FormElements;
    isRadio: boolean;
    i18n: I18nUtils;
    direction: typeof Direction;

    constructor(private $scope: IQuestionTypeSingleanswerRadioScope, private $sce: ng.ISCEService) {
        this.i18n = I18nUtils;
        this.direction = Direction;
    }

    $onInit = async () : Promise<void> => {
        for (let choice of this.question.choices.all) {
            if (choice.next_form_element_id) {
                choice.next_form_element = this.formElements.all.filter((e: FormElement) =>
                    e.id === choice.next_form_element_id &&
                    e.form_element_type === choice.next_form_element_type)[0];
            }
        }
    }

    $onDestroy = async () : Promise<void> => {}

    deleteChoice = async (index: number) : Promise<void> => {
        await this.question.deleteChoice(index);
    }

    getPosition = () : number => {
        return this.question.position ?
            this.question.position :
            this.formElements.all.filter((e: FormElement) => e.id === this.question.section_id)[0].position;
    }

    getNextElement = (position?: number) : FormElement => {
        let nextPosition: number = (position ? position : this.getPosition()) + 1;
        let nextElements: FormElement[] = this.formElements.all.filter((e: FormElement) => e.position === nextPosition);
        return nextElements.length > 0 ? nextElements[0] : null;
    }

    filterNextElements = (formElement: FormElement) : boolean => {
        let position: number = this.getPosition();
        let nextElement: FormElement = this.getNextElement(position);
        return formElement.position > position && nextElement && formElement.id != nextElement.id;
    }

    onSelectOption = (choice: QuestionChoice) : void => {
        choice.next_form_element_id = choice.next_form_element ? choice.next_form_element.id : null;
        choice.next_form_element_type = choice.next_form_element ? choice.next_form_element.form_element_type : null;
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