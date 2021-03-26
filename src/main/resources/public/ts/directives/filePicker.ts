import { ng, template, Document, $ } from 'entcore';

interface IViewModel {
    files: any,
    multiple: boolean,
    hideList: boolean,

    getTitle(title: string): string
}

export const filePicker = ng.directive('filePicker', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            files: '=',
            multiple: '@',
            hideList: '@?'
        },
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        template: `
			<div class="file-picker-list" show="display.listFiles">
				<div class="row media-library">
                    <container template="pick"></container>
				</div>
			</div>
		`,
        controller: async ($scope) => {
            const vm: IViewModel = <IViewModel> this;
        },
        link: ($scope, $element, attributes) => {
            const vm: IViewModel = $scope.vm;

            template.open('pick', 'entcore/file-picker-list/pick');

            if(!$scope.display) {
                $scope.display = {};
            }

            $scope.picked = {};
            $scope.filesArray = [];
            $scope.listFiles(vm.files);

            $('body').on('dragenter', '.icons-view', (e) => e.preventDefault());
            $('body').on('dragover', '.icons-view', (e) => e.preventDefault());
            $element.on('dragenter', (e) => e.preventDefault());

            $element.on('dragover', (e) => {
                $element.find('.drop-zone').addClass('dragover');
                e.preventDefault();
            });

            $element.on('dragleave', () => {
                $element.find('.drop-zone').removeClass('dragover');
            });

            const dropFiles = (e) => {
                if(!e.originalEvent.dataTransfer.files.length){
                    return;
                }
                $element.find('.drop-zone').removeClass('dragover');
                e.preventDefault();
                $scope.listFiles(e.originalEvent.dataTransfer.files);
                if (!$scope.hideList) {
                    $scope.display.listFiles = true;
                }
                $scope.$apply();
            }

            $scope.listFiles = function(files){
                if(!files){
                    files = $scope.picked.files;
                }
                $scope.filesArray = Array.from(files);
                vm.files = $scope.filesArray;
                if (!$scope.hideList) {
                    template.open('pick', 'entcore/file-picker-list/list');
                } else {
                    $scope.$apply();
                }
            }

            $('body').on('drop', '.icons-view', dropFiles);
            $element.on('drop', dropFiles);

            $scope.delete = (file: File) => {
                $scope.filesArray = $scope.filesArray.filter(f => f != file);
                $scope.listFiles($scope.filesArray);
                // if(scope.filesArray && scope.filesArray.length <= 0){
                //     template.open('pick', 'entcore/file-picker-list/pick');
                // }
            };

            $scope.getIconClass = (contentType) => {
                return Document.role(contentType);
            }

            $scope.getSizeHumanReadable = (size) => {
                const koSize = parseInt(size) / 1024;
                if(koSize > 1024){
                    return (koSize / 1024 * 10) / 10  + ' Mo';
                }
                return Math.ceil(koSize) + ' Ko';
            }
        }
    }
})