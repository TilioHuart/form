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
		.when('/forms-list', {
			action: 'formsList'
		})
		.when('/forms-responses', {
			action: 'formsResponses'
		})
		.when('/form/create', {
			action: 'createForm'
		})
		.when('/form/:idForm', {
			action: 'editForm'
		})
		.when('/e404', {
			action: 'e404'
		})
		.otherwise({
			redirectTo: '/forms-list'
		});
});