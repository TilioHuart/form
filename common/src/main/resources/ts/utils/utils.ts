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

    /**
     * Separate name and surname from a formatted full name
     * @param fullName full name formatted like 'NAME Surname'
     */
    static getNameAndSurname = (fullName: string) : string[] => {
        if (fullName == null || fullName.length == 0) return ["", ""];

        let lastTimeUppercaseTwice: number = -1;
        let previousLetter: string = fullName[0];
        for (let i: number = 1; i < fullName.length; i++) {
            let currentLetter: string = fullName[i];
            if (previousLetter.toLowerCase() != previousLetter.toUpperCase() &&
                previousLetter.toUpperCase() === previousLetter &&
                currentLetter.toLowerCase() != currentLetter.toUpperCase() &&
                currentLetter.toUpperCase() === currentLetter) {
                lastTimeUppercaseTwice = i;
            }
            previousLetter = fullName[i];
        }

        if (lastTimeUppercaseTwice < 0) return ["", ""];
        else {
            let name: string = fullName.substring(0, lastTimeUppercaseTwice + 1);
            let surname: string = fullName.substring(lastTimeUppercaseTwice + 2);
            return [name, surname];
        }
    }
}