import {idiom} from "entcore";

export class I18nUtils {
    static getWithParams = (key: string, params: string[]) : string => {
        let finalI18n = idiom.translate(key);
        for (let i = 0; i < params.length; i++) {
            finalI18n = finalI18n.replace(`{${i}}`, params[i]);
        }
        return finalI18n;
    };

    static getWithParam = (key: string, param: string|number) : string => {
        return I18nUtils.getWithParams(key, [param.toString()]);
    };

    static translate = (key: string) : string => {
        return idiom.translate(key);
    }
}