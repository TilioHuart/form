import {ng, $, Directive} from 'entcore';
import {FORMULAIRE_EMIT_EVENT, INFINITE_SCROLL_EVENTER} from "@common/core/enums";
import {IScope} from "angular";
import {RootsConst} from "../../core/constants/roots.const";

interface IInfiniteScrollProps {
    scrolled: any;
    loadingMode: boolean;
}

interface IViewModel {
    files: File[];
    multiple: boolean;
    loading: boolean;
}

interface IInfiniteScrollScope extends IScope, IInfiniteScrollProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    scrolled: any;
    loadingMode: boolean;
    files: File[];
    multiple: boolean;
    loading: boolean;

    constructor(private $scope: IInfiniteScrollScope, private $sce: ng.ISCEService) {
        this.loading = false;
    }

    $onInit = async () : Promise<void> => {
        $(window).on("scroll", () => {
            let currentscrollHeight: number = 0;
            // latest height once scroll will reach
            const latestHeightBottom: number = 300;
            const scrollHeight: number = $(document).height() as number;
            const scrollPos: number = Math.floor($(window).height() + $(window).scrollTop());
            const isBottom: boolean = scrollHeight - latestHeightBottom < scrollPos;

            if (isBottom && currentscrollHeight < scrollHeight) {
                if (this.$scope.loadingMode) {
                    this.loading = true;
                }
                this.$scope.$apply(this.$scope.scrolled());
                if (this.$scope.loadingMode) {
                    this.loading = false;
                }
                // Storing the latest scroll that has been the longest one in order to not redo the scrolled() each time
                currentscrollHeight = scrollHeight;
                this.$scope.$emit(FORMULAIRE_EMIT_EVENT.REFRESH);
            }

            // If somewhere in your controller you have to reinitialise anything that should "reset" your dom height
            // We reset currentscrollHeight
            this.$scope.$on(INFINITE_SCROLL_EVENTER.UPDATE, () => currentscrollHeight = 0);
        });
    }

    $onDestroy = async () : Promise<void> => {}
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}infinite-scroll/infinite-scroll.html`,
        transclude: true,
        scope: {
            scrolled: '&',
            loadingMode: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$sce', Controller],
        /* interaction DOM/element */
        link: function ($scope: IInfiniteScrollScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    };
}

export const InfiniteScroll: Directive = ng.directive('infiniteScroll', directive);