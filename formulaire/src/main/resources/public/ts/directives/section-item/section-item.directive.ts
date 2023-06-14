import {Directive, idiom, ng} from "entcore";
import {Form, FormElement, FormElements, Question, Section} from "@common/models";
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {I18nUtils} from "@common/utils";
import {RootsConst} from "../../core/constants/roots.const";
import {IScope} from "angular";
import {sectionService} from "@common/services";

interface ISectionItemProps {
    section: Section;
    reorder: boolean;
    hasFormResponses: boolean;
    formElements: FormElements;
}

interface IViewModel extends ng.IController, ISectionItemProps {
    followingFormElement: FormElement;
    i18n: I18nUtils;

    getHtmlDescription(description: string) : string;
    getTitle(title: string): string;
    editSection(): void;
    deleteSection(): void;
    isOtherElementSelected(): boolean;
    undoSectionChanges(): void;
    validateSection(): void;
    deleteSection(): void;
    addQuestionToSection(): Promise<void>;
    addQuestionToSectionGuard(): void;
    hasConditional() : boolean;
    filterNextElements(formElement: FormElement): boolean;
    onSelectOption(): Promise<void>;
}

interface ISectionItemScope extends IScope, ISectionItemProps{
    vm: IViewModel;
}

class Controller implements IViewModel {
    section: Section;
    reorder: boolean;
    hasFormResponses: boolean;
    followingFormElement: FormElement;
    formElements: FormElements;
    form: Form;
    i18n: I18nUtils;

    constructor(private $scope: ISectionItemScope, private $sce: ng.ISCEService) {
        this.i18n = I18nUtils;
    }

    $onInit = async (): Promise<void> => {
        this.section.next_form_element = this.section.getNextFormElement(this.formElements);
        this.followingFormElement = this.section.getFollowingFormElement(this.formElements);
    }

    $onDestroy = async (): Promise<void> => {}

    getHtmlDescription = (description: string) : string => {
        return !!description ? this.$sce.trustAsHtml(description) : null;
    }

    getTitle = (title: string) : string => {
        return idiom.translate('formulaire.' + title);
    }

    editSection = () : void => {
        this.section.selected = true;
    }

    deleteSection = () : void => {
        if (!this.hasFormResponses) {
            this.$scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DELETE_ELEMENT);
        }
    }

    isOtherElementSelected = () : boolean => {
        let hasSelectedChild: boolean = this.section.questions.all.filter((q: Question) => q.selected).length > 0;
        let hasSiblingChild: boolean = this.formElements.hasSelectedElement() && this.formElements.getSelectedElement().id != this.section.id;
        return hasSelectedChild || hasSiblingChild;
    }

    undoSectionChanges = () : void => {
        this.$scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.UNDO_CHANGES);
    }

    validateSection = async () : Promise<void> => {
        this.$scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.VALIDATE_SECTION);
    }

    addQuestionToSection = async () : Promise<void> => {
        if (!this.hasFormResponses) {
            this.$scope.$emit(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.CREATE_QUESTION);
        }
    }

    addQuestionToSectionGuard = () =>{
        this.addQuestionToSection().then();
    }

    hasConditional = (): boolean => {
        return this.section.questions.all.filter((q: Question) => q.conditional).length > 0;
    }

    filterNextElements = (formElement: FormElement) : boolean => {
        return formElement.position > this.section.position && this.followingFormElement && formElement.id != this.followingFormElement.id;
    }

    onSelectOption = async () : Promise<void> => {
        this.section.next_form_element_id = this.section.next_form_element ? this.section.next_form_element.id : null;
        this.section.next_form_element_type = this.section.next_form_element ? this.section.next_form_element.form_element_type : null;
        let followingFormElement: FormElement = this.section.getFollowingFormElement(this.formElements);
        this.section.is_next_form_element_default = this.section.next_form_element ?
            this.section.next_form_element.equals(followingFormElement) :
            followingFormElement == null;
        await sectionService.update([this.section]);
        this.$scope.$apply();
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}section-item/section-item.html`,
        transclude: true,
        scope: {
            section: '=',
            reorder: '=',
            hasFormResponses: '=',
            formElements: '<',
            form: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: ISectionItemScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const sectionItem: Directive = ng.directive('sectionItem', directive);