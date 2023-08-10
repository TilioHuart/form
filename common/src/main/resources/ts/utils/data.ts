import {AxiosError, AxiosResponse} from "axios";

export class DataUtils {
    static isStatusSuccess = (response: AxiosResponse) : any => {
        return response.status >= 200 && response.status < 300;
    };

    static isStatusXXX = (response: AxiosResponse, status: number) : any => {
        return response.status === status;
    };

    static getData = (response: AxiosResponse) : any => {
        if (DataUtils.isStatusSuccess(response)) { return response.data; }
        else { return null; }
    };

    static getDataIfXXX = (response: AxiosResponse, status: number) : any => {
        if (DataUtils.isStatusXXX(response, status)) { return response.data; }
        else { return null; }
    };

    static getDataIf200 = (response: AxiosResponse) : any => {
        if (DataUtils.isStatusXXX(response, 200)) { return response.data; }
        else { return null; }
    };

    static getSpecificError = (error: AxiosError) : string => {
        if (error && error.response && error.response.data && (error.response.data as any).error) {
            return (error.response.data as any).error;
        }
        return null;
    };
}