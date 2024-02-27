import {angular, idiom, ng, notify, template} from 'entcore';
import {
    Files,
    Form,
    FormElement,
    FormElements,
    Question,
    QuestionChoice,
    QuestionTypes,
    Response,
    Responses,
    Section,
    Types
} from "../models";
import {
    distributionService,
    folderService,
    formElementService,
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
} from "@common/core/enums";
import * as Sortable from "sortablejs";
import {FormElementUtils, UtilsUtils} from "@common/utils";
import {Constants} from "@common/core/constants";
import {PropPosition} from "@common/core/enums/prop-position";
import {IconUtils} from "@common/utils/icon";

enum PreviewPage { RGPD = 'rgpd', QUESTION = 'question', RECAP = 'recap'}

interface ViewModel {
    form: Form;
    formElements: FormElements;
    newElement: Question|Section;
    parentSection: Section;
    dontSave: boolean;
    nbFormElements: number;
    questionTypes: QuestionTypes;
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
        currentResponses: Map<Question, Responses>, // Responses list for preview
        currentFiles: Map<Question, Files>,
        historicPosition: number[],
        page: string,
        last: boolean
    };
    PreviewPage: typeof PreviewPage;
    nestedSortables: any[];
    iconUtils: IconUtils;
    isProcessing: boolean;

    $onInit() : Promise<void>;

    // Editor functions
    saveAll(displaySuccess?: boolean) : Promise<void>;
    return() : Promise<void>;
    createNewElement(parentSection?: Section) : Promise<void>;
    doCreateNewElement(code?: number, parentSection?: Section) : void;
    goTreeView(): void;
    organizeFormElements() : Promise<void>;
    doOrganizeFormElements() : Promise<void>;
    cancelOrganizeFormElements() : Promise<void>;
    duplicateFormElement(formElement: FormElement) : Promise<void>;
    duplicateSection(section: Section) : Promise<void>;
    duplicateQuestion(question: Question) : Promise<void>;
    deleteFormElement() : Promise<void>;
    doDeleteFormElement() : Promise<void>;
    undoFormElementChanges() : Promise<void>;
    doUndoFormElementChanges() : Promise<void>;
    validateSection(): Promise<void>;
    closeLightbox(action: string): void;
    displayTypeName(typeInfo: string) : string;
    displayTypeDescription(description : string) : string;
    moveQuestion(formElement: FormElement, direction: string) : void;

    // Preview functions
    goPreview() : void;
    backToEditor() : void;
    prev() : void;
    next() : void;
    getHtmlDescription(description: string) : string;
}


export const formEditorController = ng.controller('FormEditorController', ['$scope', '$sce',
    function ($scope, $sce) {

        const vm: ViewModel = this;
        vm.form = new Form();
        vm.formElements = new FormElements();
        vm.parentSection = null;
        vm.dontSave = false;
        vm.nbFormElements = 0;
        vm.questionTypes = new QuestionTypes();
        vm.display = {
            lightbox: {
                newElement: false,
                reorganization: false,
                delete: false,
                undo: false
            }
        };
        vm.PreviewPage = PreviewPage;
        vm.nestedSortables = [];
        vm.iconUtils = IconUtils;

        vm.$onInit = async () : Promise<void> => {
            vm.form = $scope.form;
            vm.form.nb_responses = vm.form.id ? (await distributionService.count(vm.form.id)).count : 0;
            await vm.formElements.sync(vm.form.id);
            vm.dontSave = false;
            vm.nbFormElements = vm.formElements.all.length;
            vm.questionTypes.all = vm.form.is_public ? $scope.questionTypes.all.filter(qt => qt.code != Types.FILE) : $scope.questionTypes.all;
            $scope.safeApply();

            initNestedSortables();
            $scope.safeApply();
        };

        // Global functions

        vm.saveAll = async (displaySuccess= true) : Promise<void> => {
            vm.isProcessing = true;
            vm.dontSave = true;

            let checksResult: boolean = FormElementUtils.checkFormElementsBeforeSave(vm.formElements);

            await saveFormElements(displaySuccess && checksResult);
            vm.dontSave = false;
            vm.isProcessing = false;
            $scope.safeApply();
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

        vm.createNewElement = async (parentSection?) : Promise<void> => {
            vm.isProcessing = true;
            vm.parentSection = parentSection ? parentSection : null;
            template.open('lightbox', 'lightbox/new-element');
            vm.display.lightbox.newElement = true;
            vm.isProcessing = false;
            $scope.safeApply();
        };

        vm.doCreateNewElement = async (code?, parentSection?) => {
            vm.dontSave = true;

            vm.newElement = code ? new Question() : new Section();
            if (vm.newElement instanceof Question) {
                vm.newElement.question_type = code;
                if (vm.newElement.isTypeChoicesQuestion()) {
                    for (let i = 0; i < Constants.DEFAULT_NB_CHOICES; i++) {
                        vm.newElement.choices.all.push(new QuestionChoice(vm.newElement.id, i+1));
                    }

                    if (vm.newElement.question_type === Types.MATRIX) {
                        for (let i = 0; i < Constants.DEFAULT_NB_CHILDREN; i++) {
                            vm.newElement.children.all.push(new Question(vm.newElement.id, Types.SINGLEANSWERRADIO, i+1));
                        }
                    }
                }
                if (parentSection) {
                    vm.newElement.section_id = parentSection.id;
                    vm.newElement.section_position = parentSection.questions.all.length + 1;
                    let elementSection: Section = vm.formElements.all.filter((e: FormElement) => e.id === parentSection.id)[0] as Section;
                    elementSection.questions.all.push(vm.newElement);
                }
            }

            vm.newElement.form_id = vm.form.id;
            vm.newElement.selected = true;
            if (!parentSection) {
                vm.newElement.position = vm.formElements.all.length + 1;
                vm.formElements.all.push(vm.newElement);
                vm.nbFormElements = vm.formElements.all.length;
                window.scrollTo(0, document.body.scrollHeight);
            }

            vm.parentSection = null;
            vm.display.lightbox.newElement = false;
            template.close('lightbox');
            vm.dontSave = false;
            $scope.safeApply();
        };

        vm.goTreeView = () : void => {
            $scope.redirectTo(`/form/${vm.form.id}/tree`);
        };

        vm.organizeFormElements = async () : Promise<void> => {
            await template.open('lightbox', 'lightbox/questions-reorganization');
            vm.display.lightbox.reorganization = true;
            $scope.safeApply();
            initOrgaNestedSortables();
        };

        vm.doOrganizeFormElements = async () : Promise<void> => {
            vm.isProcessing = true;
            for (let question of vm.formElements.getAllQuestions().all) {
                if ((question.section_id || question.section_position) && question.position) {
                    if (question.position > 0) {
                        question.section_id = null;
                        question.section_position = null;
                    }
                    else if (question.section_position > 0) {
                        question.position = null;
                    }
                }
            }

            let checksResult: boolean = FormElementUtils.checkFormElementsBeforeSave(vm.formElements);
            if (!checksResult) {
                vm.isProcessing = false;
                return;
            }

            await formElementService.update(vm.formElements.getAllSectionsAndAllQuestions());
            try {
                await questionChoiceService.updateMultiple(FormElementUtils.getConditionalQuestionsChoices(vm.formElements), vm.form.id);
            } catch (err) {
                notify.error(idiom.translate('formulaire.error.questionChoiceService.update'));
                throw err;
            }
            vm.display.lightbox.reorganization = false;
            vm.isProcessing = false;
            template.close('lightbox');
            $scope.safeApply();
        };

        vm.cancelOrganizeFormElements = async () : Promise<void> => {
            await vm.formElements.sync(vm.form.id);
            vm.closeLightbox('reorganization');
        };

        // FormElements functions

        vm.duplicateFormElement = async (formElement: FormElement) : Promise<void> => {
            if (formElement instanceof Question) await vm.duplicateQuestion(formElement)
            else if (formElement instanceof Section) await vm.duplicateSection(formElement)
            else notify.error(idiom.translate('formulaire.error.duplicate.type'));
        }

        vm.duplicateSection = async (section: Section) : Promise<void> => {
            try {
                vm.dontSave = true;

                // Save current edited question in current section if existing
                let selectedElements: FormElement[] = vm.formElements.selected.concat(section.questions.selected);
                if (selectedElements.length > 0) {
                    let questionToSave: FormElement = selectedElements[0];
                    if (questionToSave instanceof Question && questionToSave.section_id === section.id) {
                        await questionService.save([questionToSave]);
                        await questionChoiceService.saveMultiple(questionToSave.choices.all, vm.form.id);
                    }
                }

                // Duplicate current section
                let duplicata: Section = section;

                // Reposition form elements
                for (let i = section.position; i < vm.formElements.all.length; i++) {
                    vm.formElements.all[i].position++;
                }
                await formElementService.update(vm.formElements.all.slice(section.position));
                duplicata.position++;

                // Save duplicata section
                duplicata.setNextFormElementValuesWithDefault(vm.formElements);
                let newSection: Section = await sectionService.create(duplicata);

                // Save all questions inside section
                duplicata.questions.all.forEach((q: Question) => q.section_id = newSection.id);
                let newQuestions: Question[] = await questionService.create(duplicata.questions.all);

                // Deal with choices in questions
                let choiceToCreate: QuestionChoice[] = [];
                let childrenToCreate: Question[] = [];
                for (let question of duplicata.questions.all) {
                    if (question.isTypeChoicesQuestion()) {
                        question.choices.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);
                        let matchingNewQuestion: Question = newQuestions.find((q: Question) => q.section_position == question.section_position);
                        if (!matchingNewQuestion) continue;

                        for (let choice of question.choices.all) {
                            if (!choice.question_id) choice.question_id =  matchingNewQuestion.id;
                            if (choice.value) {
                                choiceToCreate.push(new QuestionChoice(matchingNewQuestion.id, choice.position, choice.value, choice.image, choice.is_custom));
                            }
                        }
                        if (question.question_type == Types.MATRIX) {
                            for (let child of question.children.all) {
                                child.form_id = question.form_id;
                                child.matrix_id = matchingNewQuestion.id;
                                if (child.title) {
                                    let duplicateChild: Question = new Question(matchingNewQuestion.id, child.question_type, child.matrix_position);
                                    duplicateChild.form_id = question.form_id;
                                    duplicateChild.title = child.title;
                                    childrenToCreate.push(duplicateChild);
                                }
                            }
                        }
                    }
                }
                if (choiceToCreate.length > 0) await questionChoiceService.createMultiple(choiceToCreate, vm.form.id);
                if (childrenToCreate.length > 0) await questionService.create(childrenToCreate);
                notify.success(idiom.translate('formulaire.success.section.duplicate'));
                await vm.formElements.sync(vm.form.id);
                vm.dontSave = false;
                $scope.safeApply();
            }
            catch (e) {
                throw e;
            }
        }

        vm.duplicateQuestion = async (question: Question) : Promise<void> => {
            try {
                vm.dontSave = true;
                let questionId: number = (await questionService.saveSingle(question)).id;
                let duplicata: Question = question;
                if (question.section_id) {
                    let section: Section = vm.formElements.all.filter(e => e instanceof Section && e.id === question.section_id)[0] as Section;
                    for (let i = question.section_position; i < section.questions.all.length; i++) {
                        section.questions.all[i].section_position++;
                    }
                    await formElementService.update(section.questions.all.slice(question.section_position));
                    duplicata.section_position++;
                }
                else {
                    for (let i = question.position; i < vm.formElements.all.length; i++) {
                        vm.formElements.all[i].position++;
                    }
                    await formElementService.update(vm.formElements.all.slice(question.position));
                    duplicata.position++;
                }

                let choiceToCreate: QuestionChoice[] = [];
                let childrenToCreate: Question[] = [];
                let newQuestion = await questionService.createSingle(duplicata);
                if (question.isTypeChoicesQuestion()) {
                    question.choices.all.sort((a, b) => a.position - b.position);
                    for (let choice of question.choices.all) {
                        if (!choice.question_id) choice.question_id = questionId;
                        if (choice.value) {
                            await questionChoiceService.save(choice);
                            choiceToCreate.push(new QuestionChoice(newQuestion.id, choice.position, choice.value, choice.image, choice.is_custom));
                        }
                    }
                    if (question.question_type == Types.MATRIX) {
                        for (let child of question.children.all) {
                            child.form_id = question.form_id;
                            child.matrix_id = questionId;
                            if (child.title) {
                                await questionService.saveSingle(child);
                                let duplicateChild: Question = new Question(newQuestion.id, child.question_type, child.matrix_position);
                                duplicateChild.form_id = question.form_id;
                                duplicateChild.title = child.title;
                                childrenToCreate.push(duplicateChild);
                            }
                        }
                    }
                }
                if (choiceToCreate.length > 0) await questionChoiceService.createMultiple(choiceToCreate, vm.form.id);
                if (childrenToCreate.length > 0) await questionService.create(childrenToCreate);
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
                let responseCount: number = await responseService.countByFormElement(formElement);
                if (vm.form.sent && responseCount > 0){
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
                    await formElementService.delete(formElement);
                    if (formElement instanceof Question && formElement.section_id) {
                        let section = vm.formElements.all.filter(e => e instanceof Section && e.id === (formElement as Question).section_id)[0] as Section;
                        for (let i = formElement.section_position; i < section.questions.all.length; i++) {
                            section.questions.all[i].section_position--;
                        }
                        await formElementService.update(section.questions.all.slice(formElement.section_position));
                    }
                    else {
                        for (let i = formElement.position; i < vm.formElements.all.length; i++) {
                            vm.formElements.all[i].position--;
                        }
                        await formElementService.update(vm.formElements.all.slice(formElement.position));
                    }
                }
                template.close('lightbox');
                vm.display.lightbox.delete = false;
                notify.success(idiom.translate('formulaire.success.element.delete'));
                vm.form.setFromJson(await formService.get(vm.form.id));
                await vm.formElements.sync(vm.form.id);
                vm.nbFormElements = vm.formElements.all.length;
                FormElementUtils.rePositionFormElements(vm.formElements, PropPosition.POSITION);
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

        vm.moveQuestion = (formElement: FormElement, direction: string) : void => {
            let index: number = formElement.position - 1;
            let section_index: number = formElement instanceof Question ? formElement.section_position - 1 : null;

            if (formElement instanceof Section) {
                FormElementUtils.switchPositions(vm.formElements, index, direction, PropPosition.POSITION);
            }
            else if (formElement instanceof Question) {
                let question: Question = formElement as Question;
                if (!question.section_id) {
                    let target: FormElement = direction === Direction.UP ? vm.formElements.all[index - 1] : vm.formElements.all[index + 1];
                    if (target instanceof Question) { // Switch question with question target
                        FormElementUtils.switchPositions(vm.formElements, index, direction, PropPosition.POSITION);
                    }
                    else if (target instanceof Section) {
                        switch (direction) {
                            case Direction.UP: { // Put question at the end of the section target
                                question.position = null;
                                question.section_id = target.id;
                                question.section_position = target.questions.all.length + 1;
                                FormElementUtils.updateSiblingsPositions(vm.formElements, false, null, index + 1);
                                target.questions.all.push(question);
                                vm.formElements.all = vm.formElements.all.filter(e => e.id != question.id);
                                break;
                            }
                            case Direction.DOWN: { // Put question at the start of the section target
                                question.position = null;
                                question.section_id = target.id;
                                question.section_position = 1;
                                FormElementUtils.updateSiblingsPositions(vm.formElements, false, null, index + 1);
                                FormElementUtils.updateSiblingsPositions(target.questions, true, null, 0);
                                target.questions.all.push(question);
                                vm.formElements.all = vm.formElements.all.filter(e => e.id != question.id);
                                break;
                            }
                            default:
                                notify.error(idiom.translate('formulaire.error.question.reorganization'));
                                break;
                        }
                        target.questions.all.sort((a: Question, b: Question) => a.section_position - b.section_position);
                    }
                }
                else {
                    let parentSection: Section = vm.formElements.all.filter((e: FormElement) => e.id === question.section_id)[0] as Section;
                    if (question.section_position === 1 && direction === Direction.UP) { // Take question out (before) of the parentSection
                        question.position = parentSection.position;
                        question.section_id = null;
                        question.section_position = null;
                        FormElementUtils.updateSiblingsPositions(parentSection.questions, false, null, 0);
                        FormElementUtils.updateSiblingsPositions(vm.formElements, true, null, parentSection.position - 1);
                        parentSection.questions.all = parentSection.questions.all.filter((q: Question) => q.id != question.id);
                        vm.formElements.all.push(question);
                    }
                    else if (question.section_position === parentSection.questions.all.length && direction === Direction.DOWN) { // Take question out (after) of the parentSection
                        question.position = parentSection.position + 1;
                        question.section_id = null;
                        question.section_position = null;
                        FormElementUtils.updateSiblingsPositions(parentSection.questions, false, null, parentSection.questions.all.length - 1);
                        FormElementUtils.updateSiblingsPositions(vm.formElements, true, null, parentSection.position);
                        parentSection.questions.all = parentSection.questions.all.filter((q: Question) => q.id != question.id);
                        vm.formElements.all.push(question);
                    }
                    else { // Switch two questions into the parentSection
                        FormElementUtils.switchPositions(parentSection.questions, section_index, direction, PropPosition.SECTION_POSITION);
                    }
                    parentSection.questions.all.sort((a: Question, b: Question) => a.section_position - b.section_position);
                }
            }

            vm.formElements.all.sort((a: FormElement, b: FormElement) => a.position - b.position);
            $scope.safeApply();
        };

        // Preview functions

        vm.goPreview = async () : Promise<void> => {
            await vm.saveAll(false);
            vm.preview = {
                formElement: new Question(),
                currentResponses: new Map(),
                currentFiles: new Map(),
                historicPosition: [],
                page: PreviewPage.QUESTION,
                last: false
            };

            for (let formElement of vm.formElements.all) {
                let nbQuestions: number = formElement instanceof Question ? 1 : (formElement as Section).questions.all.length;
                for (let i = 0; i < nbQuestions; i++) {
                    let question: Question = formElement instanceof Question ? formElement : (formElement as Section).questions.all[i];
                    let questionResponses: Responses = new Responses();

                    if (question.isTypeMultipleRep() || question.isRanking()) {
                        for (let choice of question.choices.all) {
                            if (question.children.all.length > 0) {
                                for (let child of question.children.all) {
                                    questionResponses.all.push(new Response(child.id, choice.id, choice.value));
                                }
                            }
                            else {
                                questionResponses.all.push(new Response(question.id, choice.id, choice.value,
                                    null, choice.position));
                            }
                        }
                    }
                    else {
                        questionResponses.all.push(new Response(question.id));
                    }

                    vm.preview.currentResponses.set(question, questionResponses);
                    if (!vm.preview.currentFiles.has(question)) vm.preview.currentFiles.set(question, new Files());
                }
            }
            if (vm.form.rgpd) {
                vm.preview.page = PreviewPage.RGPD;
            }
            else {
                vm.preview.formElement = vm.formElements.all[0];
                vm.preview.historicPosition = [1];
                vm.preview.last = vm.preview.formElement.position === vm.nbFormElements;
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
                vm.preview.historicPosition.pop();
                vm.preview.last = prevPosition === vm.nbFormElements;
                window.scrollTo(0, 0);
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
                vm.preview.page = PreviewPage.RGPD;
                $scope.safeApply();
            }
        };

        vm.next = () : void => {
            let nextPosition: number = getNextPositionIfValid();

            // Update data to display next element
            if (nextPosition && nextPosition <= vm.nbFormElements) {
                vm.preview.formElement = vm.formElements.all[nextPosition - 1];
                vm.preview.historicPosition.push(vm.preview.formElement.position);
                vm.preview.last = nextPosition === vm.nbFormElements;
                if (vm.preview.page == PreviewPage.RGPD) vm.preview.page = PreviewPage.QUESTION;
                window.scrollTo(0, 0);
                $scope.safeApply();
                // if (vm.preview.page === PreviewPage.RECAP) {
                //     vm.preview.formElement = null;
                //     vm.preview.elementResponses = null;
                //     vm.preview.page = PreviewPage.RECAP;
                // }
            }
            else if (nextPosition !== undefined) {
                vm.preview.page = PreviewPage.RECAP;
                $scope.safeApply();
            }
        };

        const getNextPositionIfValid = () : number => {
            let nextPosition: number = vm.preview.formElement.position + 1;
            let conditionalQuestion: Question = null;
            let response: Response = null;

            // Check if there are valid conditional questions and find next element position accordingly
            if (vm.preview.formElement instanceof Question && vm.preview.formElement.conditional) {
                conditionalQuestion = vm.preview.formElement;
                response = vm.preview.currentResponses.get(conditionalQuestion).all[0];
            }
            else if (vm.preview.formElement instanceof Section) {
                let conditionalQuestions = vm.preview.formElement.questions.all.filter((q: Question) => q.conditional);
                if (conditionalQuestions.length === 1) {
                    conditionalQuestion = conditionalQuestions[0];
                    response = vm.preview.currentResponses.get(conditionalQuestion).all[0];
                }
            }

            if (conditionalQuestion && response && !response.choice_id) {
                notify.info('formulaire.response.next.invalid');
                nextPosition = undefined;
            }
            else if (conditionalQuestion && response) {
                let choices: QuestionChoice[] = conditionalQuestion.choices.all.filter((c: QuestionChoice) => c.id === response.choice_id);
                let targetedElement: FormElement = choices.length === 1 ? choices[0].getNextFormElement(vm.formElements, conditionalQuestion) : null;
                nextPosition = targetedElement ? targetedElement.position : null;
            }
            else if (vm.preview.formElement instanceof Section && vm.preview.formElement.questions.all.filter((q: Question) => q.conditional).length == 0) {
                nextPosition = vm.preview.formElement.getNextFormElementPosition(vm.formElements);
            }

            return nextPosition;
        };

        vm.getHtmlDescription = (description: string) : string => {
            return !!description ? $sce.trustAsHtml(description) : null;
        }

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

                    onStart: function (evt) {
                        document.querySelector('header').style.pointerEvents = 'none';
                    },
                    onEnd: async function (evt) {
                        await FormElementUtils.onEndDragAndDrop(evt, vm.formElements);
                        document.querySelector('header').style.removeProperty('pointer-events');
                        $scope.safeApply();

                        await vm.$onInit();
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
                                vm.organizeFormElements();
                            }
                        }
                    });
                }
                $scope.safeApply();
            }, 500);
        }

        const saveFormElements = async (displaySuccess: boolean = false) : Promise<void> => {
            try {
                switchDragAndDropTo(true);
                let originalPositions: number[] = vm.formElements.all.map((e: FormElement) => e.position);
                FormElementUtils.rePositionFormElements(vm.formElements, PropPosition.POSITION);
                let formElement: FormElement = vm.formElements.getSelectedElement();
                let isSection: boolean = formElement && formElement instanceof Section;
                let isQuestionNotCursor: boolean = formElement
                    && formElement instanceof Question
                    && formElement.question_type != Types.CURSOR;
                let isCursorQuestionAndValuesOk: boolean = formElement
                    && formElement instanceof Question
                    && formElement.question_type == Types.CURSOR
                    && formElement.specific_fields.cursor_min_val != null
                    && formElement.specific_fields.cursor_max_val != null
                    && formElement.specific_fields.cursor_min_val != formElement.specific_fields.cursor_max_val;

                // Reformat positions of siblings questions of parent section if existing
                let originalSectionPositions: number[];
                let sectionParent: Section;
                if (formElement && formElement instanceof Question && formElement.section_id) {
                    sectionParent = formElement.getParentSection(vm.formElements);
                    if (sectionParent) {
                        originalSectionPositions = sectionParent.questions.all.map((q: Question) => q.section_position);
                        FormElementUtils.rePositionFormElements(sectionParent.questions, PropPosition.SECTION_POSITION);
                    }
                }

                // Reformat positions of choices if existing
                let originalChoicePositions: number[];
                if (formElement && formElement instanceof Question && formElement.choices.all.length > 0) {
                    originalChoicePositions = formElement.choices.all.map((c: QuestionChoice) => c.position);
                    formElement.choices.all.sort((a: QuestionChoice, b: QuestionChoice) => a.position - b.position);
                    for (let i: number = 0; i < formElement.choices.all.length; i++) {
                        formElement.choices.all[i][PropPosition.POSITION] = i + 1;
                    }
                }

                if (isSection || isQuestionNotCursor || isCursorQuestionAndValuesOk) {
                    // Save form element
                    let savedElement: FormElement = await formElementService.save(formElement);
                    if (!savedElement) return await vm.$onInit();
                    let newId: number = savedElement.id;
                    formElement.id = newId;

                    if (formElement instanceof Question) {
                        // Save choices
                        let registeredChoiceValues: string[] = [];
                        formElement.choices.replaceSpace();
                        let positionCounter: number = 1;
                        for (let choice of formElement.choices.all) {
                            if (!choice.value && choice.id) {
                                await questionChoiceService.delete(choice.id);
                            }
                            else if (choice.value && !registeredChoiceValues.some((v: string) => v === choice.value)) {
                                choice.position = positionCounter;
                                choice.question_id = newId;
                                choice.id = (await questionChoiceService.save(choice)).id;
                                registeredChoiceValues.push(choice.value);
                                positionCounter++;
                            }
                        }
                        // Save children (for MATRIX questions)
                        positionCounter = 1;
                        if (formElement.question_type === Types.MATRIX) {
                            let promises: Promise<any>[] = [];
                            formElement.children.all
                                .filter((q: Question) => !q.title && q.id)
                                .forEach((q: Question) => promises.push(questionService.delete(q.id)));
                            await Promise.all(promises);

                            let validatedChildren: Question[] = [];
                            for (let child of formElement.children.all) {
                                if (child.title && !validatedChildren.find((q: Question) => q.title === child.title)) {
                                    child.matrix_position = positionCounter;
                                    child.matrix_id = newId;
                                    child.form_id = formElement.form_id;
                                    validatedChildren.push(child);
                                    positionCounter++;
                                }
                            }
                            await questionService.save(validatedChildren);
                        }
                    }

                    if (displaySuccess) { notify.success(idiom.translate('formulaire.success.form.save')); }
                }
                else {
                    vm.form.setFromJson(await formService.update(vm.form));
                    if (displaySuccess) { notify.success(idiom.translate('formulaire.success.form.save')); }
                }

                // If position changes were needed we save them here
                let newPositions: number[] = vm.formElements.all.map((e: FormElement) => e.position);
                if (!UtilsUtils.areArrayEqual(originalPositions, newPositions)) {
                    await formElementService.update(vm.formElements.all);
                }
                // If section_position changes were needed we save them here
                let newSectionPositions: number[] = sectionParent ? sectionParent.questions.all.map((q: Question) => q.section_position) : null;
                if (originalSectionPositions && sectionParent && newSectionPositions && !UtilsUtils.areArrayEqual(originalSectionPositions, newSectionPositions)) {
                    await questionService.update(sectionParent.questions.all);
                }
                // If choice_position changes were needed we save them here
                let newChoicePositions: number[] = formElement instanceof Question ? formElement.choices.all.map((c: QuestionChoice) => c.position) : null;
                if (formElement instanceof Question && originalChoicePositions && newChoicePositions && !UtilsUtils.areArrayEqual(originalChoicePositions, newChoicePositions)) {
                    await questionChoiceService.updateMultiple(formElement.choices.all, vm.form.id);
                }

                await vm.$onInit();
            }
            catch (e) {
                vm.isProcessing = false;
                throw e;
            }
        };

        const onClickQuestion = async (event) : Promise<void> => {
            if (!vm.dontSave && !vm.isProcessing && $scope.currentPage === Pages.EDIT_FORM) {
                vm.isProcessing = true;
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
                vm.isProcessing = false;
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
            else if (el.classList && el.classList.contains("dontSave") || el.tagName === "LIGHTBOX" || el.tagName === "HEADER"
            || (el.tagName === "BUTTON" && el.id !== "organizeConfirm" && el.id !== "createNewEltConfirm1" && el.id !== "createNewEltConfirm2"))
            { return true; }
            return isInDontSave(el.parentNode);
        };

        document.onclick = e => { if ($scope.currentPage === Pages.EDIT_FORM) onClickQuestion(e); };

        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DUPLICATE_ELEMENT, (e) => { vm.duplicateFormElement(e.targetScope.vm.question || e.targetScope.vm.section) });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DELETE_ELEMENT, () => { vm.deleteFormElement() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.UNDO_CHANGES, () => { vm.undoFormElementChanges() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.VALIDATE_SECTION, () => { vm.validateSection() });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.CREATE_QUESTION, (e) => { vm.createNewElement(e.targetScope.vm.section) });
        $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.MOVE_QUESTION, (event, data) => { vm.moveQuestion(data.formElement, data.direction) });
        $scope.$on(FORMULAIRE_BROADCAST_EVENT.INIT_FORM_EDITOR, () => { vm.$onInit() });
    }]);