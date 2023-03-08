export class UtilsUtils {
    static safeApply = (scope, fn?) => {
        const phase = scope.$root.$$phase;
        if (phase == '$apply' || phase == '$digest') {
            if (fn && (typeof (fn) === 'function')) {
                fn();
            }
        } else {
            scope.$apply(fn);
        }
    };

    /**
     * Get a random value in a given list but excluding given values
     * @param list              List of values where to pick randomly (can be primitive, Array<primitive>, etc. but not object)
     * @param excludedValues    List of values to exclude from the random picking (should be same type as 'list')
     */
    static getRandomValueInList = (list: any[], excludedValues: any[]) : any => {
        if (list.every((item: any) => (<any>excludedValues).includes(item))) {
            return UtilsUtils.getRandomValueInList(list, []);
        }
        let randomIndex: number = Math.floor(Math.random() * list.length);
        let randomColor: string = list[randomIndex];
        return !(<any>excludedValues).includes(randomColor) ? randomColor : UtilsUtils.getRandomValueInList(list, excludedValues);
    }
}