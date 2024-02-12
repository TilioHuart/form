import {ng, Document, $} from 'entcore';
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "@common/core/enums";
import {IScope} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {UtilsUtils} from "@common/utils";

interface IFormulairePickerFileProps {
    files: File[];
    multiple: boolean;
}

interface IViewModel extends ng.IController, IFormulairePickerFileProps {
    picked: {
        files: File[]
    };
    filesArray: File[];

    listFiles(files: File[]): void;
    delete(file: File): void;
    getIconClass(contentType: string): string;
    getSizeHumanReadable(size: number): string;
}

interface IFormulairePickerFileScope extends IScope, IFormulairePickerFileProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    files: File[];
    multiple: boolean;
    picked: {
        files: File[]
    };
    filesArray: File[];

    constructor(private $scope: IFormulairePickerFileScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {
        window.setTimeout(() => {
            this.filesArray = this.files;
            UtilsUtils.safeApply(this.$scope);
            this.$scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.CHANGE_FILE_PICKER, (e, files) => {
                this.files = files;
                this.filesArray = this.files;
            });
        }, 200);
    }

    $onDestroy = async () : Promise<void> => {}

    listFiles = (files) : void => {
        if (!files){
            files = this.picked.files;
        }
        for (let i = 0; i < files.length; i++) {
            this.filesArray.push(files[i]);
        }
        this.files = this.$scope.files = this.filesArray;
        UtilsUtils.safeApply(this.$scope);
    }

    delete = (file: File) : void => {
        this.filesArray = this.filesArray.filter(f => f != file);
        this.files = this.filesArray;
    };

    getIconClass = (contentType: string) : string => {
        return Document.role(contentType);
    };

    getSizeHumanReadable = (size: number) : string => {
        const koSize = size / 1024;
        if (koSize > 1024) {
            return (koSize / 1024 * 10 / 10)  + ' Mo';
        }
        return Math.ceil(koSize) + ' Ko';
    };
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/formulaire-picker-file/formulaire-picker-file.html`,
        transclude: true,
        scope: {
            files: '=',
            multiple: '@'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IFormulairePickerFileScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            this.picked = {
                files: []
            };
            this.filesArray = [];

            const dropFiles = (e) => {
                if (!e.originalEvent.dataTransfer.files.length) return;

                element.find('.drop-zone').removeClass('dragover');
                e.preventDefault();
                vm.listFiles(e.originalEvent.dataTransfer.files);
                UtilsUtils.safeApply(this.$scope);
            }

            let body = $('body');
            body.on('dragenter', '.icons-view', (e) => e.preventDefault());
            element.on('dragenter', (e) => e.preventDefault());

            body.on('dragover', '.icons-view', (e) => e.preventDefault());
            element.on('dragover', (e) => {
                element.find('.drop-zone').addClass('dragover');
                e.preventDefault();
            });

            element.on('dragleave', () => {
                element.find('.drop-zone').removeClass('dragover');
            });

            body.on('drop', '.icons-view', dropFiles);
            element.on('drop', dropFiles);


        }
    }
}

export const formulairePickerFile = ng.directive('formulairePickerFile', directive);