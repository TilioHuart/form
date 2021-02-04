import { ng, routes } from 'entcore';
import * as controllers from './controllers';
import * as services from './services';

for(let controller in controllers){
    ng.controllers.push(controllers[controller]);
}

for (let service in services) {
	ng.services.push(services[service]);
}

routes.define(function($routeProvider){
	$routeProvider
		.when('/list', {
			action: 'list'
		})
		.when('/list/mine', {
			action: 'formsList'
		})
		.when('/list/responses', {
			action: 'formsResponses'
		})
		.when('/form/create', {
			action: 'createForm'
		})
		.when('/form/:idForm', {
			action: 'openForm'
		})
		.when('/form/:idForm/properties', {
			action: 'propForm'
		})
		.when('/form/:idForm/question/:position', {
			action: 'respondQuestion'
		})
		.when('/e403', {
			action: 'e403'
		})
		.when('/e404', {
			action: 'e404'
		})
		.otherwise({
			redirectTo: '/list'
		});
});