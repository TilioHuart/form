import {ng, template, Document, $, init} from 'entcore';
import {FORMULAIRE_BROADCAST_EVENT} from "../core/enums";

export const formulairePickerFile = ng.directive('formulairePickerFile', () => {
    return {
        restrict: 'E',
        scope: {
            files: '=',
            multiple: '@'
        },
        template: `
			<div class="file-picker-list" show="display.listFiles">
				<div class="row media-library">
                    <container template="list"></container>
                    <container template="pick"></container>
				</div>
			</div>
		`,
        link: (scope, element, attributes) => {
            template.open('pick', 'entcore/file-picker-list/pick');

            if(!scope.display) {
                scope.display = {};
            }

            scope.picked = {};
            scope.filesArray = [];

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

            const dropFiles = (e) => {
                if(!e.originalEvent.dataTransfer.files.length){
                    return;
                }
                element.find('.drop-zone').removeClass('dragover');
                e.preventDefault();
                scope.listFiles(e.originalEvent.dataTransfer.files);
                scope.$apply();
            }

            scope.listFiles = function(files){
                if (!files){
                    files = scope.picked.files;
                }
                for (let i = 0; i < files.length; i++) {
                    scope.filesArray.push(files[i]);
                }
                scope.files = scope.filesArray;
                if(scope.filesArray && scope.filesArray.length > 0){
                    template.open('list', 'entcore/file-picker-list/list');
                }
                scope.$apply();
            }

            scope.initFiles = function(files){
                scope.files = scope.filesArray = files;
                if(scope.filesArray && scope.filesArray.length > 0){
                    template.open('list', 'entcore/file-picker-list/list');
                }
                scope.$apply();
            }

            $('body').on('drop', '.icons-view', dropFiles);
            element.on('drop', dropFiles);

            scope.delete = (file: File) => {
                scope.filesArray = scope.filesArray.filter(f => f != file);
                scope.files = scope.filesArray;
                if(scope.filesArray && scope.filesArray.length <= 0){
                    template.close('list');
                }
            };

            scope.getIconClass = (contentType) => {
                return Document.role(contentType);
            }

            scope.getSizeHumanReadable = (size) => {
                const koSize = size / 1024;
                if (koSize > 1024) {
                    return (koSize / 1024 * 10 / 10)  + ' Mo';
                }
                return Math.ceil(koSize) + ' Ko';
            }

            scope.$on(FORMULAIRE_BROADCAST_EVENT.DISPLAY_FILES, (event, ctrlFiles) => { scope.initFiles(ctrlFiles) });
        }
    }
})