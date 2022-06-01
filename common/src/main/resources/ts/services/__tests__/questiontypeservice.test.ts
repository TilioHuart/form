import axios from 'axios';
import MockAdapter from "axios-mock-adapter";
import {questionTypeService} from "../QuestionTypeService";

describe('QuestionTypeService', () => {
   test('returns data in list when retrieve is correctly called', done => {
      const mock = new MockAdapter(axios);
      const data = {response: true};
      mock.onGet('/formulaire/types').reply(200, data);
      questionTypeService.list().then(response => {
         expect(response).toEqual(data);
         done();
      });
   });

   test('return data in list when retrieve is correctly called other method', done => {
      let spy = jest.spyOn(axios, "get");
      questionTypeService.list().then(response => {
         expect(spy).toHaveBeenCalledWith('/formulaire/types');
         done();
      });
   });
});