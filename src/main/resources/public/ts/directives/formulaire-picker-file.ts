import {ng, Document, $} from 'entcore';
import {FORMULAIRE_FORM_ELEMENT_EMIT_EVENT} from "../core/enums";

interface IViewModel {
    files: File[];
    multiple: boolean;

    picked: {
        files: File[]
    };
    filesArray: File[];

    $onInit() : Promise<void>;
    listFiles(files: File[]) : void;
    delete(file: File) : void;
    getIconClass(contentType: string) : string;
    getSizeHumanReadable(size: number) : string;
    $onDestroy() : Promise<void>;
}

export const formulairePickerFile = ng.directive('formulairePickerFile', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            files: '=',
            multiple: '@'
        },
        controllerAs: 'vm',
        bindToController: true,
        template: `
			<div class="file-picker-list">
				<div class="row media-library">
				
				    <!-- List files -->
				    <div class="row media-library" ng-if="vm.filesArray && vm.filesArray.length > 0">
                        <div class="loading-list drop-zone">
                            <ul>
                                <li ng-repeat="file in vm.filesArray">
                                    <div class="icon"><i ng-class="vm.getIconClass(file.type)"></i></div>
                                    <div class="title">[[file.name]]</div>
                                    <div class="status-infos">
                                        <span class="small-text horizontal-margin">[[vm.getSizeHumanReadable(file.size)]]</span>
                                    </div>
                                    <i class="close" ng-click="vm.delete(file)"></i>
                                </li>
                            </ul>
                        </div>
                    </div>
                    
				    <!-- Picker files -->
				    <div class="row media-library">
                        <div class="drop-zone import-files">
                            <article class="drop flex-row align-center">
                                <i class="two cloud-upload"></i>
                                <div class="ten help"><em><i18n>medialibrary.drop.help2</i18n></em></div>
                            </article>
                            <article class="default flex-row align-center absolute-position">
                                <div class="three select-file">
                                    <div class="hidden-content">
                                        <input type="file" ng-model="vm.picked.files" files-input-change="vm.listFiles()" multiple="multiple" />
                                    </div>
                                    <button class="file-button no-margin"><i18n>library.file.choose</i18n></button>
                                </div>
                                <i class="two cloud-upload"></i>
                                <div class="seven help"><em><i18n>medialibrary.drop.help</i18n></em></div>
                            </article>
                        </div>
                    </div>
                    
				</div>
			</div>
		`,

        controller: function ($scope) {
            const vm: IViewModel = <IViewModel> this;

            vm.$onInit = async () : Promise<void> => {
                window.setTimeout(() => {
                    console.log("init files with : ", vm.files);
                    $scope.files = vm.filesArray = vm.files;
                    $scope.$apply();
                }, 200);
            };
        },
        link: function ($scope, element, attributes) {
            const vm: IViewModel = $scope.vm;

            vm.picked = {
                files: []
            };
            vm.filesArray = [];

            const dropFiles = (e) => {
                if(!e.originalEvent.dataTransfer.files.length){
                    return;
                }
                element.find('.drop-zone').removeClass('dragover');
                e.preventDefault();
                vm.listFiles(e.originalEvent.dataTransfer.files);
                $scope.$apply();
            }

            vm.listFiles = (files) : void => {
                if (!files){
                    files = vm.picked.files;
                }
                for (let i = 0; i < files.length; i++) {
                    vm.filesArray.push(files[i]);
                }
                vm.files = $scope.files = vm.filesArray;
                $scope.$apply();
            }

            vm.delete = (file: File) : void => {
                vm.filesArray = vm.filesArray.filter(f => f != file);
                vm.files = $scope.files = vm.filesArray;
            };

            vm.getIconClass = (contentType) : string => {
                return Document.role(contentType);
            };

            vm.getSizeHumanReadable = (size) : string => {
                const koSize = size / 1024;
                if (koSize > 1024) {
                    return (koSize / 1024 * 10 / 10)  + ' Mo';
                }
                return Math.ceil(koSize) + ' Ko';
            };

            $('body').on('dragenter', '.icons-view', (e) => e.preventDefault());
            $('body').on('dragover', '.icons-view', (e) => e.preventDefault());
            element.on('dragenter', (e) => e.preventDefault());

            element.on('dragover', (e) => {
                element.find('.drop-zone').addClass('dragover');
                e.preventDefault();
            });

            element.on('dragleave', () => {
                element.find('.drop-zone').removeClass('dragover');
            });

            $('body').on('drop', '.icons-view', dropFiles);
            element.on('drop', dropFiles);

            vm.$onDestroy = async () : Promise<void> => {
                console.log("in destroy");
                $scope.files = vm.filesArray = vm.files = [];
                $scope.$apply();
            };

            $scope.$on(FORMULAIRE_FORM_ELEMENT_EMIT_EVENT.DESTROY_FILE_PICKER, () => { vm.$onDestroy(); });
        }
    }
})