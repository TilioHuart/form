import axios from 'axios';
import MockAdapter from "axios-mock-adapter";
import {distributionService} from "../DistributionService";

describe('DistributionService', () => {
   test('returns data when API request is correctly called for listByFormAndStatusAndQuestion method', done => {
      const formId = 1;
      const questionId = 1;
      const status = "status";
      const nbLines = 10;
      const mock = new MockAdapter(axios);
      const data = { response: true };
      mock.onGet(`/formulaire/distributions/forms/${formId}/questions/${questionId}/list/${status}?nbLines=${nbLines}`).reply(200, data);
      distributionService.listByFormAndStatusAndQuestion(formId, status, questionId, nbLines).then(response => {
         expect(response).toEqual(data);
         done();
      });
   });
});