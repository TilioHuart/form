import {Response, Responses} from "@common/models";
import {angular} from "entcore";

export class RankingUtils {
    static onEndRankingDragAndDrop = (evt: any, responses: Responses): boolean => {
        let elem = evt.item.firstElementChild.firstElementChild;
        let scopElem = angular.element(elem).scope().vm;
        let itemId = scopElem.question.id;
        let item: Response = (responses as Responses).all.filter(q => q.question_id === itemId)[0] as Response;
        let oldIndex: number = evt.oldIndex;
        let newIndex: number = evt.newIndex;
        let indexes: any = RankingUtils.getStartEndIndexes(newIndex, oldIndex);

        RankingUtils.updateChoicePositions(responses, indexes.goUp, indexes.startIndex, indexes.endIndex);
        item.choice_position = newIndex + 1;

        responses.all.sort((a: Response, b: Response) => a.choice_position - b.choice_position);
        return false;
    }

    static updateChoicePositions = (responses: Responses, goUp: boolean, startIndex: number, endIndex?: number): void => {
        endIndex = endIndex ? endIndex : responses.all.length;
        for (let i = startIndex; i < endIndex; i++) {
            let elt = responses.all[i];
            goUp ? elt.choice_position++ : elt.choice_position--;
        }
    };

    static getStartEndIndexes = (newIndex: number, oldIndex: number) : any => {
        let indexes = {startIndex: -1, endIndex: -1, goUp: false};
        if (newIndex < oldIndex) {
            indexes.goUp = true;
            indexes.startIndex = newIndex;
            indexes.endIndex = oldIndex;
        }
        else {
            indexes.goUp = false;
            indexes.startIndex = oldIndex;
            indexes.endIndex = newIndex + 1;
        }
        return indexes;
    }
}