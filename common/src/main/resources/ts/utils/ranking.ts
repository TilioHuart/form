import {Responses} from "@common/models";

export class RankingUtils {
    static onEndRankingDragAndDrop = (evt: any, responses: Responses): void => {
        let oldIndex: number = evt.oldIndex;
        let newIndex: number = evt.newIndex;

        // Move the item to its new position
        responses.all.splice(newIndex, 0, responses.all.splice(oldIndex, 1)[0]);

        // Update the choice positions of the items
        for (let i = 0; i < responses.all.length; i++) {
            let elt = responses.all[i];
            elt.choice_position = i + 1;
        }
    }
}
