import {idiom, ng, notify, template, angular} from 'entcore';
import {
    Form,
    FormElement,
    FormElements,
    Question,
    QuestionChoice,
    Response,
    Responses,
    Section,
    Types
} from "../models";
import {
    distributionService, formElementService,
    formService,
    questionChoiceService,
    questionService,
    responseService,
    sectionService
} from "../services";
import {
    Direction,
    FORMULAIRE_BROADCAST_EVENT,
    FORMULAIRE_EMIT_EVENT,
    FORMULAIRE_FORM_ELEMENT_EMIT_EVENT,
    Pages
} from "../core/enums";
import {folderService} from "../services/FolderService";

enum PreviewPage { RGPD = 'rgpd', QUESTION = 'question', RECAP = 'recap'}

interface ViewModel {
    form: Form;
    formElements: FormElements;
    newElement: Question|Section;
    parentSection: Section;
    dontSave: boolean;
    nbFormElements: number;
    last: boolean;
    display: {
        lightbox: {
            newElement: boolean,
            reorganization: boolean,
            delete: boolean,
            undo: boolean
        }
    };
    preview: {
        formElement: FormElement, // Question for preview
        formResponses: Responses[], // Responses list for preview
        elementResponses: Responses, // Response for preview
        files: File[],
        page: string
    };
    PreviewPage: typeof PreviewPage;

    $onInit() : Promise<void>;

    // Editor functions
    saveAll(displaySuccess?: boolean) : Promise<void>;
    return() : Promise<void>;
    createNewElement(parentSection?: Section) : void;
    doCreateNewElement(code?: number, parentSection?: Section) : void;
    organizeQuestions() : void;
    doOrganizeQuestions() : Promise<void>;
    cancelOrganizeQuestions() : Promise<void>;
    duplicateQuestion(question: Question) : Promise<void>;
    deleteFormElement() : Promise<void>;
    doDeleteFormElement() : Promise<void>;
    undoFormElementChanges();
    doUndoFormElementChanges() : Promise<void>;
    validateSection(): Promise<void>;
    closeLightbox(action: string): void;
    displayTypeName(typeInfo: string) : string;
    displayTypeDescription(description : string) : string;
    displayTypeIcon(code: number) : string;
    reOrder() : void;
    moveQuestion(index: number, direction: string) : void;

    // Preview functions
    goPreview() : void;
    backToEditor() : void;
    prev() : void;
    next() : void;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope',
    function ($scope) {

        const vm: ViewModel = this;
        vm.form = new Form();
        vm.formElements = new FormElements();
        vm.parentSection = null;
        vm.dontSave = false;
        vm.nbFormElements = 0;
        vm.last = false;
        vm.display = {
            lightbox: {
                newElement: false,
                reorganization: false,
                delete: false,
                undo: false
            }
        };
        vm.preview = {
            formElement: new Question(),
            formResponses: [],
            elementResponses: new Responses(),
            files: [],
            page: PreviewPage.QUESTION
        };
        vm.PreviewPage = PreviewPage;

        vm.$onInit = async () : Promise<void> => {
            vm.form = $scope.form;
            vm.form.nb_responses = vm.form.id ? (await distributionService.count(vm.form.id)).count : 0;
            await vm.formElements.sync(vm.form.id);
            vm.dontSave = false;
            vm.nbFormElements = vm.formElements.all.length;
            $scope.safeApply();
        };

        // Global functions

        vm.saveAll = async (displaySuccess= true) : Promise<void> => {
            vm.dontSave = true;
            let wrongElements = vm.formElements.all.filter(fe => !fe.title); // TODO check more than just titles later
            if (wrongElements.length > 0) {
                notify.error(idiom.translate('formulaire.question.save.missing.field'));
            }
            await saveFormElements(displaySuccess && wrongElements.length <= 0);
            vm.dontSave = false;
        };

        vm.return = async() : Promise<void> => {
            vm.dontSave = true;
            let wrongElements = vm.formElements.all.filter(question => !question.title); // TODO check more than just titles later
            if (wrongElements.length > 0) {
                notify.error(idiom.translate('formulaire.question.save.missing.field'));
                vm.dontSave = false;
            } else {
                let folder = await folderService.get(vm.form.folder_id);
                $scope.$emit(FORMULAIRE_EMIT_EVENT.UPDATE_FOLDER, folder);
                $scope.redirectTo('/list/mine');
            }
        };

        vm.createNewElement = (parentSection?) => {
            vm.parentSection = parentSection ? parentSection : null;
            template.open('lightbox', 'lightbox/new-element');
            vm.display.lightbox.newElement = true;
            $scope.safeApply();
        };

        vm.doCreateNewElement = async (code?, parentSection?) => {
            vm.dontSave = true;

            vm.newElement = code ? new Question() : new Section();
            if (vm.newElement instanceof Question) {
                vm.newElement = new Question();
                vm.newElement.question_type = code;
                if (vm.newElement.question_type === Types.MULTIPLEANSWER
                    || vm.newElement.question_type === Types.SINGLEANSWER
                    || vm.newElement.question_type === Types.SINGLEANSWERRADIO) {
                    for (let i = 0; i < 3; i++) {
                        vm.newElement.choices.all.push(new QuestionChoice());
                    }
                }
                if (parentSection) {
                    vm.newElement.section_id = parentSection.id;
                    vm.newElement.section_position = parentSection.questions.all.length + 1;
                    parentSection.questions.all.push(vm.newElement);
                }
            }
            else {
                vm.newElement = new Section();
            }

            vm.newElement.form_id = vm.form.id;
            vm.newElement.selected = true;
            if (!parentSection) {
                vm.newElement.position = vm.formElements.all.length + 1;
                vm.formElements.all.push(vm.newElement);
                vm.nbFormElements = vm.formElements.all.length;
            }

            vm.parentSection = null;
            vm.display.lightbox.newElement = false;
            template.close('lightbox');
            vm.dontSave = false;
            $scope.safeApply();
        };

        vm.organizeQuestions = () : void => {
            template.open('lightbox', 'lightbox/questions-reorganization');
            vm.display.lightbox.reorganization = true;
            $scope.safeApply();
        };

        vm.doOrganizeQuestions = async () : Promise<void> => {
            await saveFormElements();
            vm.display.lightbox.reorganization = false;
            template.close('lightbox');
            $scope.safeApply();
        };

        vm.cancelOrganizeQuestions = async () : Promise<void> => {
            await vm.formElements.sync(vm.form.id);
            vm.closeLightbox('reorganization');
        };

        // FormElements functions

        vm.duplicateQuestion = async (question: Question) : Promise<void> => {
            try {
                vm.dontSave = true;
                await questionService.save(question);
                let duplicata = question;
                if (question.section_id) {
                    let section = vm.formElements.all.filter(e => e instanceof Section && e.id === question.section_id)[0] as Section;
                    for (let i = question.section_position; i < section.questions.all.length; i++) {
                        section.questions.all[i].position++;
                        await formElementService.save(section.questions.all[i]);
                    }
                    duplicata.section_position++;
                }
                else {
                    for (let i = question.position; i < vm.formElements.all.length; i++) {
                        vm.formElements.all[i].position++;
                        await formElementService.save(vm.formElements.all[i]);
                    }
                    duplicata.position++;
                }
                let newQuestion = await questionService.create(duplicata);
                if (question.question_type === Types.SINGLEANSWER
                    || question.question_type === Types.MULTIPLEANSWER
                    || question.question_type === Types.SINGLEANSWERRADIO) {
                    for (let choice of question.choices.all) {
                        if (choice.value) {
                            await questionChoiceService.create(new QuestionChoice(newQuestion.id, choice.value));
                        }
                    }
                }
                notify.success(idiom.translate('formulaire.success.question.duplicate'));
                await vm.formElements.sync(vm.form.id);
                vm.dontSave = false;
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        };

        vm.deleteFormElement = async () : Promise<void> => {
            let formElement = vm.formElements.getSelectedElement();
            if (formElement.id) {
                let responseCount = await responseService.countByQuestion(formElement.id);
                if (vm.form.sent && responseCount.count > 0){
                    notify.error(idiom.translate('formulaire.question.delete.response.fill.warning'));
                }
                else if (vm.form.sent && vm.formElements.all.length === 1) {
                    notify.error(idiom.translate('formulaire.question.delete.empty.warning'));
                }
                else {
                    vm.dontSave = true;
                    template.open('lightbox', 'lightbox/question-confirm-delete');
                    vm.display.lightbox.delete = true;
                }
            }
            else if (formElement instanceof Question && formElement.section_id) {
                let section = vm.formElements.getSections().all.filter(s => s.id === formElement.id)[0];
                section.questions.all = section.questions.all.filter(q => q.id);
            }
            else {
                vm.formElements.all = vm.formElements.all.filter(e => e.id);
            }
        };

        vm.doDeleteFormElement = async () : Promise<void> => {
            try {
                let formElement = vm.formElements.getSelectedElement();
                if (formElement.id) {
                    await formElementService.delete(formElement);
                }
                template.close('lightbox');
                vm.display.lightbox.delete = false;
                notify.success(idiom.translate('formulaire.success.question.delete'));
                await vm.formElements.sync(vm.form.id);
                vm.nbFormElements = vm.formElements.all.length;
                rePositionQuestions();
                vm.dontSave = false;
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        };

        vm.undoFormElementChanges = async () => {
            if (vm.formElements.selected.length > 0) {
                let formElement = vm.formElements.selected[0];
                let hasChanged = false;
                if (formElement instanceof Question) {
                    hasChanged = (!!formElement.title || !!formElement.statement || formElement.mandatory || formElement.choices.all.length > 0);
                }
                else if (formElement instanceof Section) {
                    hasChanged = (!!formElement.title || !!formElement.description || formElement.questions.all.length > 0);
                }

                if (hasChanged) {
                    vm.dontSave = true;
                    template.open('lightbox', 'lightbox/question-confirm-undo');
                    vm.display.lightbox.undo = true;
                }
                else {
                    await vm.formElements.sync(vm.form.id);
                    $scope.safeApply();
                }
            }
        };

        vm.doUndoFormElementChanges = async () : Promise<void> => {
            await vm.formElements.sync(vm.form.id);
            template.close('lightbox');
            vm.display.lightbox.undo = true;
            vm.dontSave = false;
            $scope.safeApply();
        };

        vm.validateSection = async () : Promise<void> => {
            let formElement = vm.formElements.selected[0];
            if (formElement instanceof Section) {
                await sectionService.save(formElement);
                await vm.formElements.sync(vm.form.id);
                $scope.safeApply();
            }
        };

        vm.closeLightbox = (action: string) : void => {
            template.close('lightbox');
            vm.display.lightbox[action] = false;
            vm.dontSave = false;
            $scope.safeApply();
        };

        // Display functions

        vm.displayTypeName = (typeInfo: string|number) : string => {
            if (typeof typeInfo === "string") {
                return idiom.translate('formulaire.question.type.' + typeInfo);
            }
            else if (typeof typeInfo === "number") {
                let name = $scope.getTypeNameByCode(typeInfo);
                return idiom.translate('formulaire.question.type.' + name);
            }
            else {
                return "ERROR_TEXT";
            }
        };

        vm.displayTypeDescription = (description : string|number) :string => {
            return idiom.translate("formulaire.question.type.description." + description);
        };

        vm.displayTypeIcon = (code: number) : string => {
            switch (code) {
                case 1 :
                    return "/formulaire/public/img/question_type/long-answer.svg";
                case 2 :
                    return "/formulaire/public/img/question_type/short-answer.svg";
                case 3 :
                    return "/formulaire/public/img/question_type/free-text.svg";
                case 4 :
                    return "/formulaire/public/img/question_type/unic-answer.svg";
                case 5 :
                    return "/formulaire/public/img/question_type/multiple-answer.svg";
                case 6 :
                    return "/formulaire/public/img/question_type/date.svg";
                case 7 :
                    return "/formulaire/public/img/question_type/time.svg";
                case 8 :
                    return "/formulaire/public/img/question_type/file.svg";
                case 9:
                    return "/formulaire/public/img/question_type/singleanswer_radio.svg";
            }
        };

        vm.reOrder = () : void => {
            let finished = true;
            angular.forEach(vm.formElements.all, function (question) {
                if (question.position != parseFloat(question.index) + 1) {
                    question.position = parseFloat(question.index) + 1;
                }
                if (!question.position) { finished = false }
            });
            if (finished) {
                rePositionQuestions();
            }
        };

        vm.moveQuestion = (index: number, direction: string) : void => {
            switch (direction) {
                case Direction.UP: {
                    vm.formElements.all[index].position--;
                    vm.formElements.all[index - 1].position++;
                    break;
                }
                case Direction.DOWN: {
                    vm.formElements.all[index].position++;
                    vm.formElements.all[index + 1].position--;
                    break;
                }
                default:
                    notify.error(idiom.translate('formulaire.error.question.reorganization'));
                    break;
            }
            rePositionQuestions();
            $scope.safeApply();
        };

        // Preview functions

        vm.goPreview = async () : Promise<void> => {
            await vm.saveAll(false);
            vm.preview.formResponses = [];
            for (let formElement of vm.formElements.all) {
                let responses = new Responses();
                if (formElement instanceof Question) {
                    let response = new Response();
                    if (formElement.question_type === Types.MULTIPLEANSWER || formElement.question_type === Types.SINGLEANSWERRADIO) {
                        response.selectedIndex = new Array<boolean>(formElement.choices.all.length);
                    }
                    responses.all.push(response);
                }
                vm.preview.formResponses.push(responses);
            }
            if (vm.form.rgpd) {
                vm.preview.page = PreviewPage.RGPD;
            }
            else {
                vm.preview.formElement = vm.formElements.all[0];
                vm.preview.elementResponses = vm.preview.formResponses[0];
                vm.last = vm.preview.formElement.position === vm.nbFormElements;
                vm.preview.page = PreviewPage.QUESTION;
            }
            $scope.currentPage = Pages.PREVIEW;
            $scope.safeApply();
        };

        vm.backToEditor = () : void => {
            $scope.currentPage = Pages.EDIT_FORM;
            $scope.safeApply();
        };

        vm.prev = () : void => {
            if (vm.preview.page === PreviewPage.RECAP) {
                vm.preview.formElement = vm.formElements.all[vm.nbFormElements - 1];
                vm.preview.elementResponses = vm.preview.formResponses[vm.nbFormElements - 1];
                vm.last = vm.preview.formElement.position === vm.nbFormElements;
                vm.preview.page = PreviewPage.QUESTION;
            }
            else {
                let prevPosition: number = vm.preview.formElement.position - 1;
                vm.last = prevPosition === vm.nbFormElements;
                if (prevPosition > 0) {
                    vm.preview.formElement = vm.formElements.all[prevPosition - 1];
                    vm.preview.elementResponses = vm.preview.formResponses[prevPosition - 1];
                }
                else if (vm.form.rgpd) {
                    vm.preview.formElement = null;
                    vm.preview.elementResponses = null;
                    vm.preview.page = PreviewPage.RGPD;
                }
            }
            $scope.safeApply();
        };

        vm.next = () : void => {
            if (vm.preview.page === PreviewPage.RGPD) {
                vm.preview.formElement = vm.formElements.all[0];
                vm.preview.elementResponses = vm.preview.formResponses[0];
                vm.last = vm.preview.formElement.position === vm.nbFormElements;
                vm.preview.page = PreviewPage.QUESTION;
            }
            else {
                let nextPosition: number = vm.preview.formElement.position + 1;
                vm.last = nextPosition === vm.nbFormElements;
                if (nextPosition <= vm.nbFormElements) {
                    vm.preview.formElement = vm.formElements.all[nextPosition - 1];
                    vm.preview.elementResponses = vm.preview.formResponses[nextPosition - 1];
                }
                else {
                    // TODO init what needed for recap page
                    vm.preview.formElement = null;
                    vm.preview.elementResponses = null;
                    vm.preview.page = PreviewPage.RECAP;
                }
            }
            $scope.safeApply();
        };

        // Utils

        const rePositionQuestions = () : void => {
            vm.formElements.all.sort((a, b) => a.position - b.position);
            for (let i = 0; i < vm.formElements.all.length; i++) {
                vm.formElements.all[i].position = i + 1;
            }
            $scope.safeApply();
        };

        const saveFormElements = async (displaySuccess: boolean = false) : Promise<void> => {
            try {
                rePositionQuestions();
                let formElement = vm.formElements.getSelectedElement();
                if (formElement instanceof Question && !formElement.title && !formElement.statement && !formElement.mandatory && formElement.choices.all.length <= 0) {
                    if (formElement.id) {
                        vm.formElements.all.filter(e => e.id)[0] = await questionService.get(formElement.id);
                    }
                }
                else if (formElement instanceof Section && !formElement.title && !formElement.description && formElement.questions.all.length <= 0) {
                    if (formElement.id) {
                        vm.formElements.all.filter(e => e.id)[0] = await sectionService.get(formElement.id);
                    }
                }
                else {
                    let newId = (await formElementService.save(formElement)).id;
                    formElement.id = newId;

                    if (formElement instanceof Question) {
                        let registeredChoices = [];
                        formElement.choices.replaceSpace();
                        for (let choice of formElement.choices.all) {
                            if (choice.value && !registeredChoices.find(c => c === choice.value)) {
                                choice.question_id = newId;
                                choice.id = (await questionChoiceService.save(choice)).id;
                                registeredChoices.push(choice.value);
                            }
                        }
                    }
                }
                if (displaySuccess) { notify.success(idiom.translate('formulaire.success.form.save')); }
                vm.form.setFromJson(await formService.get(vm.form.id));
                vm.formElements.deselectAll();
                for (let section of vm.formElements.getSections().all) {
                    section.questions.deselectAll();
                }
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        };

        const onClickQuestion = async (event) : Promise<void> => {
            if (!vm.dontSave && $scope.currentPage === Pages.EDIT_FORM) {
                let question = isInFocusable(event.target);
                if (question && question.id && question.id > 0) {
                    if (!question.selected) {
                        if (vm.formElements.hasSelectedElement()) {
                            await saveFormElements();
                        }
                        // Reselection of the question because the sync has removed the selections
                        question.selected = true;
                    }
                }
                else if (question && !question.id) {
                    question.selected = true;
                }
                else if(!isInDontSave(event.target)) {
                    await saveFormElements();
                }
                $scope.safeApply();
            }
        };

        const isInShowErrorZone = (el) : boolean => {
            if (!el) { return true; }
            else if (el.classList && el.classList.contains("dontShowError")) { return false; }
            return isInShowErrorZone(el.parentNode);
        };

        const isInFocusable = (el) : Question => {
            if (!el) { return null; }
            else if (el.classList && el.classList.contains("focusable")) { return angular.element(el).scope().vm.question; }
            return isInFocusable(el.parentNode);
        };

        const isInDontSave = (el) : boolean => {
            if (!el) { return false; }
            else if (el.classList && el.classList.contains("dontSave")) { return true; }
            return isInDontSave(el.parentNode);
        };

        document.onclick = e => { onClickQuestion(e); };

        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DUPLICATE_ELEMENT, (e) => { vm.duplicateQuestion(e.targetScope.vm.question) });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DELETE_ELEMENT, () => { vm.deleteFormElement() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.UNDO_CHANGES, () => { vm.undoFormElementChanges() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.VALIDATE_SECTION, () => { vm.validateSection() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.CREATE_QUESTION, (e) => { vm.createNewElement(e.targetScope.vm.section) });
        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_FORM_EDITOR, () => { vm.$onInit() });
    }]);