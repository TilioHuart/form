import {idiom, ng, notify} from 'entcore';
import http from 'axios';
import {DataUtils} from "@common/utils";

export interface CaptchaService {
    getCaptcha(distributionKey: string, distributionCaptcha?: string) : Promise<any>;
}

export const captchaService: CaptchaService = {
    async getCaptcha(distributionKey: string, distributionCaptcha?: string) : Promise<any> {
        try {
            return DataUtils.getData(await http.get(`/formulaire-public/captcha/${distributionKey}?captcha_id=${distributionCaptcha}`));
        } catch (err) {
            notify.error(idiom.translate('formulaire.public.error.captchaService.get'));
            throw err;
        }
    }
};

export const CaptchaService = ng.service('CaptchaService', (): CaptchaService => captchaService);