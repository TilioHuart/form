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
        let randomIndex: number = Math.floor(Math.random() * list.length);
        return !(<any>excludedValues).includes(list[randomIndex]) ? list[randomIndex] : UtilsUtils.getRandomValueInList(list, excludedValues);
    }
}