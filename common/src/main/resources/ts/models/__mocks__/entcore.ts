declare let require: any;

export const moment = require('moment');

interface IController {
    name: string,
    contents: any
}
interface IDirective {
    name: string,
    contents: any
}
interface IService {
    name: string,
    contents: any
}

const controllers: Array<IController> = [];
const directives: Array<IDirective> = [];
const services: Array<IService> = [];

export const ng = {
    service: jest.fn((name: string, contents: any) => {
        ng.services.push({name, contents})
    }),
    directive: jest.fn((name: string, contents: any) => {
        ng.directives.push({name, contents})
    }),
    controller: jest.fn((name:string, contents:any) => {
        ng.controllers.push({name, contents})
    }),
    initMockedModules: jest.fn((app:any) => {
        ng.services.forEach((s: IService) => app.service(s.name, s.contents));
        ng.directives.forEach((d: IDirective)=> app.directive(d.name, d.contents));
        ng.controllers.forEach((c: IController)=> app.controller(c.name, c.contents));
    }),
    controllers: controllers,
    directives: directives,
    services: services
};

export const notify = {
    error: jest.fn()
}

export const idiom = {
    translate: jest.fn()
}

