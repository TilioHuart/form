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
import {distributionService, formElementService, formService, questionChoiceService, questionService, responseService, sectionService} from "../services";
import {
    Direction,
    FORMULAIRE_BROADCAST_EVENT,
    FORMULAIRE_EMIT_EVENT,
    FORMULAIRE_FORM_ELEMENT_EMIT_EVENT,
    Pages
} from "../core/enums";
import {folderService} from "../services/FolderService";
import * as Sortable from "sortablejs";
import {FormElementUtils} from "../utils";

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
        choices: Responses,
        historicPosition: number[],
        page: string
    };
    PreviewPage: typeof PreviewPage;
    nestedSortables: any[];

    $onInit() : Promise<void>;

    // Editor functions
    saveAll(displaySuccess?: boolean) : Promise<void>;
    return() : Promise<void>;
    createNewElement(parentSection?: Section) : Promise<void>;
    createNewElementGuard() : void;
    doCreateNewElement(code?: number, parentSection?: Section) : void;
    organizeQuestions() : Promise<void>;
    organizeQuestionsGuard() : void;
    doOrganizeQuestions() : Promise<void>;
    cancelOrganizeQuestions() : Promise<void>;
    duplicateQuestion(question: Question) : Promise<void>;
    deleteFormElement() : Promise<void>;
    doDeleteFormElement() : Promise<void>;
    undoFormElementChanges() : Promise<void>;
    undoFormElementChangesGuard(): void;
    doUndoFormElementChanges() : Promise<void>;
    validateSection(): Promise<void>;
    closeLightbox(action: string): void;
    displayTypeName(typeInfo: string) : string;
    displayTypeDescription(description : string) : string;
    displayTypeIcon(code: number) : string;
    moveQuestion(formElement: FormElement, direction: string) : void;

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
            choices: new Responses(),
            historicPosition: [],
            page: PreviewPage.QUESTION
        };
        vm.PreviewPage = PreviewPage;
        vm.nestedSortables = [];

        vm.$onInit = async () : Promise<void> => {
            vm.form = $scope.form;
            vm.form.nb_responses = vm.form.id ? (await distributionService.count(vm.form.id)).count : 0;
            await vm.formElements.sync(vm.form.id);
            vm.dontSave = false;
            vm.nbFormElements = vm.formElements.all.length;
            $scope.safeApply();

            initNestedSortables();
            $scope.safeApply();
        };

        // Global functions

        vm.saveAll = async (displaySuccess= true) : Promise<void> => {
            vm.dontSave = true;
            let sections = vm.formElements.getSections();
            let sectionQuestions = sections.all.filter(s => s.id);
            let questionsList = sectionQuestions.map(s => s.questions.all);

            for (let questions of questionsList){
                let conditionalQuestions = questions.filter(q => q.conditional);
                if (conditionalQuestions.length >= 2) {
                    notify.error(idiom.translate('formulaire.question.save.missing.field'));
                    return;
                }
            }

            let wrongElements = vm.formElements.all.filter(fe => !fe.title); // TODO check more than just titles later
            if (wrongElements.length > 0) {
                notify.error(idiom.translate('formulaire.question.save.missing.field'));
                return;
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

        vm.createNewElement = async (parentSection?) : Promise<void>=> {
            vm.parentSection = parentSection ? parentSection : null;
            template.open('lightbox', 'lightbox/new-element');
            vm.display.lightbox.newElement = true;
            $scope.safeApply();
        };

        vm.createNewElementGuard = () => {
            vm.createNewElement().then();
        }

        vm.doCreateNewElement = async (code?, parentSection?) => {
            vm.dontSave = true;

            vm.newElement = code ? new Question() : new Section();
            if (vm.newElement instanceof Question) {
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
                    let elementSection = vm.formElements.all.filter(e => e.id === parentSection.id)[0] as Section;
                    elementSection.questions.all.push(vm.newElement);
                }
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

        vm.organizeQuestions = async () : Promise<void> => {
            await template.open('lightbox', 'lightbox/questions-reorganization');
            vm.display.lightbox.reorganization = true;
            $scope.safeApply();
            initOrgaNestedSortables();
        };

        vm.organizeQuestionsGuard = () => {
            vm.organizeQuestions().then();
        }

        vm.doOrganizeQuestions = async () : Promise<void> => {
            await updateAllFormElements();
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
                    }
                    await formElementService.update(section.questions.all);
                    duplicata.section_position++;
                }
                else {
                    for (let i = question.position; i < vm.formElements.all.length; i++) {
                        vm.formElements.all[i].position++;
                    }
                    await formElementService.update(vm.formElements.all);
                    duplicata.position++;
                }
                let newQuestion = await questionService.create(duplicata);
                if (question.question_type === Types.SINGLEANSWER
                    || question.question_type === Types.MULTIPLEANSWER
                    || question.question_type === Types.SINGLEANSWERRADIO) {
                    question.choices.all.sort((a, b) => a.id - b.id);
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
                let responseCount = await responseService.countByFormElement(formElement);
                if (vm.form.sent && responseCount.count > 0){
                    notify.error(idiom.translate('formulaire.element.delete.response.fill.warning'));
                }
                else if (vm.form.sent && vm.formElements.all.length === 1) {
                    notify.error(idiom.translate('formulaire.element.delete.empty.warning'));
                }
                else {
                    vm.dontSave = true;
                    template.open('lightbox', 'lightbox/question-confirm-delete');
                    vm.display.lightbox.delete = true;
                }
            }
            else if (formElement instanceof Question && formElement.section_id) {
                let section = vm.formElements.getSections().all.filter(s => s.id === (formElement as Question).section_id)[0];
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
                    if (formElement instanceof Question && formElement.section_id) {
                        let section = vm.formElements.all.filter(e => e instanceof Section && e.id === (formElement as Question).section_id)[0] as Section;
                        for (let i = formElement.section_position; i < section.questions.all.length; i++) {
                            section.questions.all[i].position--;
                        }
                        await formElementService.update(section.questions.all);
                    }
                    else {
                        for (let i = formElement.position; i < vm.formElements.all.length; i++) {
                            vm.formElements.all[i].position--;
                        }
                        await formElementService.update(vm.formElements.all);
                    }
                    await formElementService.delete(formElement);
                }
                template.close('lightbox');
                vm.display.lightbox.delete = false;
                notify.success(idiom.translate('formulaire.success.element.delete'));
                vm.form.setFromJson(await formService.get(vm.form.id));
                await vm.formElements.sync(vm.form.id);
                vm.nbFormElements = vm.formElements.all.length;
                rePositionFormElements(vm.formElements);
                vm.dontSave = false;
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        };

        vm.undoFormElementChanges = async () : Promise<void> => {
            let formElement = vm.formElements.getSelectedElement();
            if (formElement) {
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

        vm.undoFormElementChangesGuard = () => {
            vm.undoFormElementChanges().then();
        }

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

        vm.moveQuestion = (formElement: FormElement, direction: string) : void => {
            let index = formElement.position - 1;
            let section_index = formElement instanceof Question ? formElement.section_position - 1 : null;

            if (formElement instanceof Section) {
                FormElementUtils.switchPositions(vm.formElements, index, direction);
            }
            else if (formElement instanceof Question) {
                let question = formElement as Question;
                if (!question.section_id) {
                    let target = direction === Direction.UP ? vm.formElements.all[index - 1] : vm.formElements.all[index + 1];
                    if (target instanceof Question) { // Switch question with question target
                        FormElementUtils.switchPositions(vm.formElements, index, direction);
                    }
                    else if (target instanceof Section) { // Put question into section target (start or end)
                        switch (direction) {
                            case Direction.UP: {
                                question.position = null;
                                question.section_id = target.id;
                                question.section_position = target.questions.all.length + 1;
                                FormElementUtils.updateSiblingsPositions(vm.formElements, false, null, index);
                                target.questions.all.push(question);
                                vm.formElements.all = vm.formElements.all.filter(e => e.id != question.id);
                                break;
                            }
                            case Direction.DOWN: {
                                question.position = null;
                                question.section_id = target.id;
                                question.section_position = 1;
                                FormElementUtils.updateSiblingsPositions(vm.formElements, false, null, index);
                                FormElementUtils.updateSiblingsPositions(target.questions, true, null, 0);
                                target.questions.all.push(question);
                                vm.formElements.all = vm.formElements.all.filter(e => e.id != question.id);
                                break;
                            }
                            default:
                                notify.error(idiom.translate('formulaire.error.question.reorganization'));
                                break;
                        }
                        target.questions.all.sort((a, b) => a.section_position - b.section_position);
                    }
                }
                else {
                    let parentSection = vm.formElements.all.filter(e => e.id === question.section_id)[0] as Section;
                    if (question.section_position === 1 && direction === Direction.UP) { // Take question out (before) of the parentSection
                        question.position = parentSection.position;
                        question.section_id = null;
                        question.section_position = null;
                        FormElementUtils.updateSiblingsPositions(parentSection.questions, false, null, 0);
                        FormElementUtils.updateSiblingsPositions(vm.formElements, true, null, parentSection.position - 1);
                        parentSection.questions.all = parentSection.questions.all.filter(q => q.id != question.id);
                        vm.formElements.all.push(question);
                    }
                    else if (question.section_position === parentSection.questions.all.length && direction === Direction.DOWN) { // Take question out (after) of the parentSection

                        question.position = parentSection.position + 1;
                        question.section_id = null;
                        question.section_position = null;
                        FormElementUtils.updateSiblingsPositions(parentSection.questions, false, null, parentSection.questions.all.length - 1);
                        FormElementUtils.updateSiblingsPositions(vm.formElements, true, null, parentSection.position - 1);
                        parentSection.questions.all = parentSection.questions.all.filter(q => q.id != question.id);
                        vm.formElements.all.push(question);
                    }
                    else { // Switch two questions into the parentSection
                        FormElementUtils.switchPositions(parentSection.questions, section_index, direction);
                    }
                    parentSection.questions.all.sort((a, b) => a.section_position - b.section_position);
                }
            }

            vm.formElements.all.sort((a, b) => a.position - b.position);
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
                vm.preview.historicPosition = [1];
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
            let prevPosition = vm.preview.historicPosition[vm.preview.historicPosition.length - 2];
            if (prevPosition > 0) {
                vm.preview.formElement = vm.formElements.all[prevPosition - 1];
                vm.preview.elementResponses = vm.preview.formResponses[prevPosition - 1];
                vm.preview.historicPosition.pop();
                vm.last = prevPosition === vm.nbFormElements;
                $scope.safeApply();
                // if (vm.preview.page === PreviewPage.RECAP) {
                //     vm.preview.formElement = vm.formElements.all[vm.nbFormElements - 1];
                //     vm.preview.elementResponses = vm.preview.formResponses[vm.nbFormElements - 1];
                //     vm.last = vm.preview.formElement.position === vm.nbFormElements;
                //     vm.preview.page = PreviewPage.QUESTION;
                // }
            }
            else if (vm.form.rgpd) {
                vm.preview.formElement = null;
                vm.preview.elementResponses = null;
                vm.preview.page = PreviewPage.RGPD;
            }
            $scope.safeApply();
        };

        vm.next = () : void => {
            let nextPosition: number = vm.preview.formElement.position + 1;
            let conditionalQuestion = null;
            let response = null;

            if (vm.preview.formElement instanceof Question && vm.preview.formElement.conditional) {
                conditionalQuestion = vm.preview.formElement;
                response = vm.preview.elementResponses.all[0];
            }
            else if (vm.preview.formElement instanceof Section) {
                let conditionalQuestions = vm.preview.formElement.questions.all.filter(q => q.conditional);
                if (conditionalQuestions.length === 1) {
                    conditionalQuestion = conditionalQuestions[0];
                    response = vm.preview.elementResponses.all[conditionalQuestion.section_position - 1];
                }
            }

            if (conditionalQuestion && response && !response.choice_id) {
                notify.info('formulaire.response.next.invalid');
                nextPosition = null;
            }
            else if (conditionalQuestion && response) {
                let choices = conditionalQuestion.choices.all.filter(c => c.id === response.choice_id);
                let sectionId = choices.length === 1 ? choices[0].next_section_id : null;
                let filteredSections = vm.formElements.getSections().all.filter(s => s.id === sectionId);
                let targetSection = filteredSections.length === 1 ? filteredSections[0] : null;
                nextPosition = targetSection ? targetSection.position : null;
            }

            if (nextPosition && nextPosition <= vm.nbFormElements) {
                vm.preview.formElement = vm.formElements.all[nextPosition - 1];
                vm.preview.historicPosition.push(vm.preview.formElement.position);
                vm.preview.elementResponses = vm.preview.formResponses[nextPosition - 1];
                vm.last = nextPosition === vm.nbFormElements;
                if (vm.preview.page == PreviewPage.RGPD) {
                    vm.preview.historicPosition = [1];
                    vm.preview.page = PreviewPage.QUESTION;
                }
                // if (vm.preview.page === PreviewPage.RECAP) {
                //     vm.preview.formElement = null;
                //     vm.preview.elementResponses = null;
                //     vm.preview.page = PreviewPage.RECAP;
                // }
            }
            else if (nextPosition && nextPosition <= vm.nbFormElements) {
                vm.backToEditor();
            }
            else if (nextPosition != undefined) {
                vm.preview.page = PreviewPage.RECAP;
            }
            $scope.safeApply();
        };

        // Utils

        const initNestedSortables = () : void => {
            // Loop through each nested sortable element for DragAndDrop of questions
            for (let i = 0; i < vm.nestedSortables.length; i++) {
                vm.nestedSortables[i].destroy();
            }
            vm.nestedSortables = [];
            let nestedSortables = document.querySelectorAll(".nested-container");
            for (let i = 0; i < nestedSortables.length; i++) {
                vm.nestedSortables.push(Sortable.create(nestedSortables[i], {
                    group: 'nested',
                    animation: 150,
                    fallbackOnBody: true,
                    swapThreshold: 0.65,
                    ghostClass: "sortable-ghost",
                    onEnd: async function (evt) {
                        let cleanResidue = await FormElementUtils.onEndDragAndDrop(evt, vm.formElements);
                        $scope.safeApply();

                        await vm.$onInit();
                        if (cleanResidue) {
                            document.getElementById('container-0').lastElementChild.remove();
                            $scope.safeApply();
                        }
                    }
                }));
            }
        }

        const switchDragAndDropTo = (state: boolean) : void => {
            for (let i = 0; i < vm.nestedSortables.length; i++) {
                vm.nestedSortables[i].option("disabled", !state);
            }
            $scope.safeApply();
        }

        const initOrgaNestedSortables = () : void => {
            // Loop through each nested sortable element for DragAndDrop in pop reorganization
            window.setTimeout(() : void => {
                let orgaNestedSortables = document.querySelectorAll(".orga-nested-container");
                for (let i = 0; i < orgaNestedSortables.length; i++) {
                    Sortable.create(orgaNestedSortables[i], {
                        group: 'orga-nested',
                        animation: 150,
                        fallbackOnBody: true,
                        swapThreshold: 0.65,
                        ghostClass: "sortable-ghost",
                        onEnd: async function (evt) {
                            let refresh = await FormElementUtils.onEndOrgaDragAndDrop(evt, vm.formElements);
                            $scope.safeApply();
                            if (refresh) {
                                await vm.formElements.sync(vm.form.id);
                                vm.organizeQuestionsGuard();
                            }
                        }
                    });
                }
                $scope.safeApply();
            }, 500);
        }

        const rePositionFormElements = (formElements: FormElements) : void => {
            formElements.all.sort((a, b) => a.position - b.position);
            for (let i = 0; i < formElements.all.length; i++) {
                formElements.all[i].position = i + 1;
            }
            $scope.safeApply();
        };

        const updateAllFormElements = async () : Promise<void> => {
            await formElementService.update(vm.formElements.all);
            for (let section of vm.formElements.getSections().all) {
                await formElementService.update(section.questions.all);
            }
        };

        const saveFormElements = async (displaySuccess: boolean = false) : Promise<void> => {
            try {
                switchDragAndDropTo(true);
                rePositionFormElements(vm.formElements);
                let formElement = vm.formElements.getSelectedElement();
                if (formElement) {
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
                        let test = await formElementService.save(formElement);
                        let newId = test.id;
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
                }
                else {
                    vm.form.setFromJson(await formService.update(vm.form));
                    if (displaySuccess) { notify.success(idiom.translate('formulaire.success.form.save')); }
                }
                await vm.$onInit();
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
                        switchDragAndDropTo(false);
                    }
                }
                else if (question && !question.id) {
                    question.selected = true;
                    switchDragAndDropTo(false);
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

        document.onclick = e => { if ($scope.currentPage === Pages.EDIT_FORM) onClickQuestion(e); };

        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DUPLICATE_ELEMENT, (e) => { vm.duplicateQuestion(e.targetScope.vm.question) });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DELETE_ELEMENT, () => { vm.deleteFormElement() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.UNDO_CHANGES, () => { vm.undoFormElementChangesGuard() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.VALIDATE_SECTION, () => { vm.validateSection() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.CREATE_QUESTION, (e) => { vm.createNewElement(e.targetScope.vm.section) });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.MOVE_QUESTION, (event, data) => { vm.moveQuestion(data.formElement, data.direction) });
        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_FORM_EDITOR, () => { vm.$onInit() });
    }]);