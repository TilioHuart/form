import { ng, routes } from 'entcore';
import * as controllers from './controllers';
import * as services from './services';
import * as directives from './directives';

for(let controller in controllers){
    ng.controllers.push(controllers[controller]);
}

for (let service in services) {
	ng.services.push(services[service]);
}

for (let directive in directives) {
	ng.directives.push(directives[directive]);
}

routes.define(function($routeProvider){
	$routeProvider
		.when('/list', { action: 'list' })
		.when('/list/mine', { action: 'formsList' })
		.when('/list/responses', { action: 'formsResponses' })
		.when('/form/create', { action: 'createForm' })
		.when('/form/:idForm/edit', { action: 'editForm' })
		.when('/form/:idForm', { action: 'respondForm' })
		.when('/form/:idForm/properties', { action: 'propForm' })
		.when('/form/:idForm/results/empty', { action: 'emptyResults' })
		.when('/form/:idForm/results/:position', { action: 'resultsForm' })
		.when('/form/:idForm/question/:position', { action: 'respondQuestion' })
		.when('/form/:idForm/questions/recap', { action: 'recapQuestions' })
		.when('/e403', { action: 'e403' })
		.when('/e404', { action: 'e404' })
		.when('/e409', { action: 'e409' })
		.otherwise({ redirectTo: '/list' });
});