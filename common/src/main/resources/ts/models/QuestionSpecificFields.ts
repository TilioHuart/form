import {Question} from "@common/models";

export class QuestionSpecificFields {
    id: number
    question_id: number;
    cursor_min_val: number;
    cursor_max_val: number;
    cursor_step: number;
    cursor_min_label: string;
    cursor_max_label: string;

    constructor(questionId?: number) {
        this.question_id = questionId;
        this.cursor_min_val = null;
        this.cursor_max_val = null;
        this.cursor_step = null;
        this.cursor_min_label = null;
        this.cursor_max_label = null;
    }

    build(data: IQuestionSpecificFieldsResponse) : QuestionSpecificFields {
        this.id = data.id ? data.id : null;
        this.question_id = data.questionId != null ? data.questionId : null;
        this.cursor_min_val = data.cursorMinVal != null ? data.cursorMinVal : null;
        this.cursor_max_val = data.cursorMaxVal != null ? data.cursorMaxVal : null;
        this.cursor_step = data.cursorStep != null ? data.cursorStep : null;
        this.cursor_min_label = data.cursorMinLabel ? data.cursorMinLabel : null;
        this.cursor_max_label = data.cursorMaxLabel ? data.cursorMaxLabel : null;
        return this;
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            cursor_min_val: this.cursor_min_val,
            cursor_max_val: this.cursor_max_val,
            cursor_step: this.cursor_step,
            cursor_min_label: this.cursor_min_label,
            cursor_max_label: this.cursor_max_label
        }
    }
}

export class QuestionSpecificFieldsPayload {
    id: number
    question_id: number;
    cursor_min_val: number;
    cursor_max_val: number;
    cursor_step: number;
    cursor_min_label: string;
    cursor_max_label: string;

    constructor(question: Question) {
        if (!question.specific_fields) return;
        this.cursor_min_val = question.specific_fields.cursor_min_val != null ? question.specific_fields.cursor_min_val : null;
        this.cursor_max_val = question.specific_fields.cursor_max_val != null ? question.specific_fields.cursor_max_val : null;
        this.cursor_step = question.specific_fields.cursor_step != null ? question.specific_fields.cursor_step : null;
        this.cursor_min_label = question.specific_fields.cursor_min_label != null ? question.specific_fields.cursor_min_label : null;
        this.cursor_max_label = question.specific_fields.cursor_max_label != null ? question.specific_fields.cursor_max_label : null;
    }

    toJson() : Object {
        return {
            id: this.id,
            question_id: this.question_id,
            cursor_min_val: this.cursor_min_val,
            cursor_max_val: this.cursor_max_val,
            cursor_step: this.cursor_step,
            cursor_min_label: this.cursor_min_label,
            cursor_max_label: this.cursor_max_label
        }
    }
}

export interface IQuestionSpecificFieldsResponse {
    id: number;
    questionId: number;
    cursorMinVal: number;
    cursorMaxVal: number;
    cursorStep: number;
    cursorMinLabel: string;
    cursorMaxLabel: string;
}