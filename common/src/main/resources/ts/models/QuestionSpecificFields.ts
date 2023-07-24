import {Question} from "@common/models";

export class QuestionSpecificFieldsPayload {
    cursor_min_val: number;
    cursor_max_val: number;
    cursor_step: number;
    cursor_min_label: string;
    cursor_max_label: string;

    constructor(question: Question) {
        this.cursor_min_val = question.cursor_min_val ? question.cursor_min_val : null;
        this.cursor_max_val = question.cursor_max_val ? question.cursor_max_val : null;
        this.cursor_step = question.cursor_step ? question.cursor_step : null;
        this.cursor_min_label = question.cursor_min_label ? question.cursor_min_label : null;
        this.cursor_max_label = question.cursor_max_label ? question.cursor_max_label : null;
    }

    toJson() : Object {
        return {
            cursor_min_val: this.cursor_min_val,
            cursor_max_val: this.cursor_max_val,
            cursor_step: this.cursor_step,
            cursor_min_label: this.cursor_min_label,
            cursor_max_label: this.cursor_max_label
        }
    }
}