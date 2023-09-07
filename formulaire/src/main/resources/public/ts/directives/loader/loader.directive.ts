import {Directive, ng} from 'entcore';
import {IScope} from "angular";
import {RootsConst} from "../../core/constants/roots.const";

interface ILoaderProps {
    title: string;
    minHeight: number;
}

interface IViewModel {}

interface ILoaderScope extends IScope, ILoaderProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    title: string;
    minHeight: number;

    constructor(private $scope: ILoaderScope, private $sce: ng.ISCEService) {}

    $onInit = async () : Promise<void> => {}

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}loader/loader.html`,
        scope: {
            title: '=',
            minHeight: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: ILoaderScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    };
}

export const Loader: Directive = ng.directive('loader', directive);
