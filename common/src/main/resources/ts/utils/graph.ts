import {idiom} from 'entcore';
import {Question, QuestionChoice, Types} from "@common/models";
import {ColorUtils} from "@common/utils/color";
import * as ApexCharts from 'apexcharts';

export class GraphUtils {

    // Results page
    /**
     *  Generate data, options and render graph of the results of a question according to its type (for result page view)
     * @param question  Question object which we want to display the results
     * @param chart     ApexChart to render at the end
     */
    static generateGraphForResult = (question: Question, chart: any) : void => {
        switch (question.question_type) {
            case Types.SINGLEANSWER:
            case Types.SINGLEANSWERRADIO:
                GraphUtils.generateSingleAnswerChart(question, chart);
                break;
            case Types.MATRIX:
                GraphUtils.generateMatrixChart(question, chart);
                break;
            default:
                break;
        }
    };

    /**
     * Generate and render graph of the results of a single answer question (for result page view)
     * @param question  Question object which we want to display the results
     * @param chart     ApexChart to render at the end
     */
    private static generateSingleAnswerChart = (question: Question, chart: any) : void => {
        let choices: QuestionChoice[] = question.choices.all.filter((c: QuestionChoice) => c.nbResponses > 0);

        let series: number[] = [];
        let labels: string[] = [];
        let i18nValue: string = idiom.translate('formulaire.response');
        i18nValue = i18nValue.charAt(0).toUpperCase() + i18nValue.slice(1);

        for (let choice of choices) {
            series.push(choice.nbResponses); // Fill data
            let index: number = question.choices.all.indexOf(choice) + 1;
            !choice.id ? labels.push(idiom.translate('formulaire.response.empty')) : labels.push(i18nValue + " " + index); // Fill labels
        }

        // Generate options with labels and colors
        let baseHeight: number = 40 * question.choices.all.length;
        let height: number = baseHeight < 200 ? 200 : (baseHeight > 500 ? 500 : baseHeight);

        let colors: string[] = ColorUtils.generateColorList(labels.length);
        let newOptions: any = GraphUtils.generateOptions(question.question_type, colors, labels, height, '100%');
        newOptions.series = series;

        GraphUtils.renderChartForResult(newOptions, chart, question);
    };

    /**
     * Generate and render graph of the results of a matrix question (for result page view)
     * @param question  Question object which we want to display the results
     * @param chart     ApexChart to render at the end
     */
    private static generateMatrixChart = (question: Question, chart: any) : any => {
        let choices: QuestionChoice[] = question.choices.all;

        let series: any[] = [];
        let labels: string[] = question.children.all.map((child: Question) => child.title);

        for (let choice of choices) {
            let serie: any = {
                name: choice.value,
                data: []
            };

            // Fill serie data with nb responses of this choice for this child question
            for (let child of question.children.all) {
                let matchingChoice: QuestionChoice[] = child.choices.all.filter((c: QuestionChoice) => c.value === choice.value);
                serie.data.push(matchingChoice.length == 1 ? matchingChoice[0].nbResponses : 0);
            }

            series.push(serie); // Fill series
        }

        // Generate options with labels and colors
        let colors: string[] = ColorUtils.generateColorList(series.length);
        let newOptions: any = GraphUtils.generateOptions(question.question_type, colors, labels, '100%', '100%');
        newOptions.series = series;

        GraphUtils.renderChartForResult(newOptions, chart, question);
    };

    /**
     * Render graph with ApexChart based on given options (for result page view)
     * @param options   ApexChart options to render the graph
     * @param chart     ApexChart to render at the end
     * @param question  Question object which we want to display the results
     */
    static renderChartForResult = (options: any, chart: any, question: Question) : void => {
        chart = new ApexCharts(document.querySelector(`#chart-${question.id}`), options);
        chart.render();
    };


    // PDF export

    /**
     * Generate data, options and render graph of the results of a question according to its type (for PDF)
     * @param question  Question object which we want to display the results
     * @param charts    ApexCharts to store and render at the end
     */
    static generateGraphForPDF = async (question: Question, charts: any, nbDistribs: number) : Promise<void> => {
        switch (question.question_type) {
            case Types.SINGLEANSWER:
            case Types.SINGLEANSWERRADIO:
                await GraphUtils.generateSingleAnswerChartForPDF(question, charts);
                break;
            case Types.MULTIPLEANSWER:
                await GraphUtils.generateMultipleAnswerChartForPDF(question, charts, nbDistribs);
                break;
            case Types.MATRIX:
                await GraphUtils.generateMatrixChartForPDF(question, charts);
                break;
            default:
                break;
        }
    };

    /**
     * Generate and render graph of the results of a single answer question (for PDF)
     * @param question  Question object which we want to display the results
     * @param charts    ApexCharts to store and render at the end
     */
    static generateSingleAnswerChartForPDF = async (question: Question, charts: any) : Promise<void> => {
        if (question.question_type != Types.SINGLEANSWER && question.question_type != Types.SINGLEANSWERRADIO) {
            return null;
        }

        let choices: QuestionChoice[] = question.choices.all.filter((c: QuestionChoice) => c.nbResponses > 0);

        let series: number[] = [];
        let labels: string[] = [];

        for (let choice of choices) {
            series.push(choice.nbResponses); // Fill data
            // Fill labels
            !choice.id ?
                labels.push(idiom.translate('formulaire.response.empty')) :
                labels.push(choice.value.substring(0, 40) + (choice.value.length > 40 ? "..." : ""));
        }

        let colors: string[] = ColorUtils.generateColorList(labels.length);
        let newOptions: any = GraphUtils.generateOptions(question.question_type, colors, labels);
        newOptions.series = series;

        await GraphUtils.renderChartForPDF(newOptions, charts);
    }

    /**
     * Generate and render graph of the results of a multiple answers question (for PDF)
     * @param question  Question object which we want to display the results
     * @param charts    ApexCharts to store and render at the end
     */
    static generateMultipleAnswerChartForPDF = async (question: Question, charts: any, distribs: number) : Promise<void> => {
        if (question.question_type != Types.MULTIPLEANSWER) {
            return null;
        }

        let choices: QuestionChoice[] = question.choices.all;

        let series: number[] = [];
        let labels: string[] = [];
        let seriesPercent: number[] = [];

        for (let choice of choices) {
            series.push(choice.nbResponses); // Fill data
            // Fill labels
            !choice.id ?
                labels.push(idiom.translate('formulaire.response.empty')) :
                labels.push(choice.value.substring(0, 40) + (choice.value.length > 40 ? "..." : ""))
            seriesPercent.push((choice.nbResponses/distribs)*100)
        }

        let colors: string[] = ColorUtils.generateColorList(labels.length);
        let newOptions: any = GraphUtils.generateOptions(question.question_type, colors, labels, null, null,
            seriesPercent);
        newOptions.series = [{ data: series }];

        await GraphUtils.renderChartForPDF(newOptions, charts);
    }

    /**
     * Generate and render graph of the results of a matrix question (for PDF)
     * @param question  Question object which we want to display the results
     * @param charts    ApexCharts to store and render at the end
     */
    static generateMatrixChartForPDF = async (question: Question, charts: any) : Promise<void> => {
        if (question.question_type != Types.MATRIX) {
            return null;
        }

        let choices: QuestionChoice[] = question.choices.all;

        let series: any[] = [];
        let labels: string[] = question.children.all.map((child: Question) => child.title);

        for (let choice of choices) {
            let serie: any = {
                name: choice.value,
                data: []
            };

            // Fill serie data with nb responses of this choice for this child question
            for (let child of question.children.all) {
                let matchingChoice: QuestionChoice[] = child.choices.all.filter((c: QuestionChoice) => c.value === choice.value);
                serie.data.push(matchingChoice.length == 1 ? matchingChoice[0].nbResponses : 0);
            }

            series.push(serie); // Fill series
        }

        let colors: string[] = ColorUtils.generateColorList(series.length);
        let newOptions: any = GraphUtils.generateOptions(question.question_type, colors, labels);
        newOptions.series = series;

        await GraphUtils.renderChartForPDF(newOptions, charts);
    }

    /**
     * Render graph with ApexChart based on given options (for PDF)
     * @param options   ApexChart options to render the graph
     * @param charts    ApexCharts to store and render at the end
     */
    static renderChartForPDF = async (options: any, charts: any) : Promise<void> => {
        charts.push(new ApexCharts(document.querySelector(`#pdf-response-chart-${charts.length}`), options));
        await charts[charts.length - 1].render();
    };


    // Options generation for graphs

    /**
     *  Generate and return ApexCharts options according to the type of the question to display
     * @param type      Type of the question
     * @param colors    Colors to use for the graph
     * @param labels    Labels to display on the cart
     * @param height    Height of the chart to display (optional)
     * @param width     Width of the chart to display (optional)
     * @param seriesPercent Percentage to use for the graph
     */
    static generateOptions = (type: Types, colors: string[], labels: string[], height?: any, width?: any, seriesPercent?: number[]) : any => {
        let options: any;
        if (type === Types.SINGLEANSWER || type === Types.SINGLEANSWERRADIO) {
            options = {
                chart: {
                    type: 'pie',
                    height: height ? height : 400,
                    width: width ? width : 600,
                    animations: {
                        enabled: false
                    }
                },
                colors: colors,
                labels: labels
            }
        }
        else if (type === Types.MULTIPLEANSWER) {
            options = {
                chart: {
                    type: 'bar',
                    height: height ? height : 400,
                    width: width ? width : 600,
                    animations: {
                        enabled: false
                    }
                },
                plotOptions: {
                    bar: {
                        borderRadius: 4,
                        horizontal: true,
                        distributed: true
                    }
                },
                dataLabels: {
                    enable: true,
                    formatter: function (val: number, opt: any): string {
                        return (val + " (" + seriesPercent[opt.dataPointIndex].toFixed(2)  + "%)")
                    },
                },
                colors: colors,
                xaxis: {
                    categories: labels,
                }
            }
        }
        else if (type === Types.MATRIX) {
            options = {
                chart: {
                    type: 'bar',
                    height: height ? height : 400,
                    width: width ? width : 600,
                    animations: {
                        enabled: false
                    }
                },
                colors: colors,
                dataLabels: {
                    enabled: false
                },
                xaxis: {
                    categories: labels,
                },
                fill: {
                    opacity: 1
                }
            };
        }
        return options;
    }
}