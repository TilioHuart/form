import {idiom, ng, notify, template} from "entcore";
import {DistributionStatus, Form, Question, Response, Responses} from "@common/models";
import {captchaService, responseService} from "../services";
import {Mix} from "entcore-toolkit";

interface ViewModel {
    form: Form;
    formKey: string;
    distributionKey: string;
    distributionCaptcha: string;
    responses: Responses;
    captcha: Question;
    responseCaptcha: Response;
    display: {
        lightbox: {
            sending: boolean
        }
    };
    isProcessing: boolean;

    $onInit() : Promise<void>;
    send() : void;
    doSend() : Promise<void>;
    backToRecap() : void;
    generateNewCaptcha() : Promise<void>;
}

export const captchaController = ng.controller('CaptchaController', ['$scope',
    function ($scope) {

    const vm: ViewModel = this;
    vm.captcha = new Question();
    vm.responseCaptcha = new Response();
    vm.responses = new Responses();
    vm.display = {
        lightbox: {
            sending: false
        }
    };

    vm.$onInit = async () : Promise<void> => {
        syncWithStorageData();
        vm.captcha = Mix.castAs(Question, await captchaService.getCaptcha(vm.distributionKey, vm.distributionCaptcha));
        $scope.safeApply();
    };

    // Global functions

    vm.backToRecap = () : void => {
        template.open('main', 'containers/recap');
    };

    vm.generateNewCaptcha = async () : Promise<void> => {
        vm.responseCaptcha = new Response();
        let captcha = await captchaService.getCaptcha(vm.distributionKey);
        sessionStorage.setItem('distributionCaptcha', JSON.stringify(captcha.captcha_id));
        vm.captcha = Mix.castAs(Question, captcha);
        $scope.safeApply();
    };

    vm.send = () : void => {
        template.open('lightbox', 'lightbox/responses-confirm-sending');
        vm.display.lightbox.sending = true;
    };

    vm.doSend = async () : Promise<void> => {
        vm.isProcessing = true;
        let distributionData = await responseService.sendResponses(vm.formKey, vm.distributionKey, vm.responseCaptcha, vm.responses);
        let distribution = Mix.castAs(Question, distributionData);

        template.close('lightbox');
        vm.display.lightbox.sending = false;

        if (distribution.status != DistributionStatus.FINISHED) {
            notify.error(idiom.translate('formulaire.public.error.captcha'));
            await vm.generateNewCaptcha();
        }
        else {
            notify.success(idiom.translate('formulaire.public.success.responses.save'));
            window.setTimeout(function () {
                sessionStorage.clear();
                template.open('main', 'containers/end/thanks');
            }, 1000);
        }
        vm.isProcessing = false;
    };

    // Utils

    const syncWithStorageData = () : void => {
        vm.form = Mix.castAs(Form, JSON.parse(sessionStorage.getItem('form')));
        vm.formKey = JSON.parse(sessionStorage.getItem('formKey'));
        vm.distributionKey = JSON.parse(sessionStorage.getItem('distributionKey'));
        vm.distributionCaptcha = JSON.parse(sessionStorage.getItem('distributionCaptcha'));
        vm.responses.all = JSON.parse(sessionStorage.getItem('responses')).arr;
    };
}]);