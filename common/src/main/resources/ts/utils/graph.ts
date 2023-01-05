import {idiom, idiom as lang} from 'entcore';
import {Question, QuestionChoice, Response, Types} from "@common/models";
import {ColorUtils} from "@common/utils/color";
import * as ApexCharts from 'apexcharts';

export class GraphUtils {

    // Results page
    /**
     * Generate data, options and render graph of the results of a question according to its type (for result page view)
     * @param question    Question object which we want to display the results
     * @param responses   Array of responses which we want to display the results
     * @param isExportPDF Boolean to determine if we generate a graph for result or for PDF Export
     * @param charts      ApexCharts to store and render at the end
     * @param distribs  Distrib's number for each question
     */
    static generateGraphForResult = async (question: Question, charts: ApexChart[], responses: Response[],
                                           distribs: number, isExportPDF: boolean) : Promise<void> => {
        switch (question.question_type) {
            case Types.SINGLEANSWER:
            case Types.SINGLEANSWERRADIO:
                await GraphUtils.generateSingleAnswerChart(question, charts, isExportPDF);
                break;
            case Types.MULTIPLEANSWER:
                await GraphUtils.generateMultipleAnswerChart(question, charts, distribs, isExportPDF);
                break;
            case Types.MATRIX:
                await GraphUtils.generateMatrixChart(question, charts, isExportPDF);
                break;
            case Types.CURSOR:
                await GraphUtils.generateCursorChart(question, charts, responses, isExportPDF);
                break;
            default:
                break;
        }
    };

    /**
     * Generate and render graph of the results of a single answer question
     * @param question    Question object which we want to display the results
     * @param charts      ApexChart to render at the end
     * @param isExportPDF Boolean to determine if we generate a graph for result or for PDF Export
     */
    private static generateSingleAnswerChart = async (question: Question, charts: ApexChart[], isExportPDF: boolean) : Promise<void> => {
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

        let newOptions: any = isExportPDF ?
            GraphUtils.generateOptions(question.question_type, colors, labels)
            :
            GraphUtils.generateOptions(question.question_type, colors, labels, height, '100%');

        newOptions.series = series;

        await GraphUtils.renderChartForResult(newOptions, charts, question, isExportPDF);
    };

    /**
     * Generate and render graph of the results of a matrix question
     * @param question  Question object which we want to display the results
     * @param charts     ApexChart to render at the end
     * @param isExportPDF Boolean to determine if we generate a graph for result or for PDF Export
     */
    private static generateMatrixChart = async (question: Question, charts: ApexChart[], isExportPDF: boolean) : Promise<void> => {
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

        let newOptions: any = isExportPDF ?
            GraphUtils.generateOptions(question.question_type, colors, labels, null, null)
            :
            GraphUtils.generateOptions(question.question_type, colors, labels, '100%', '100%');

        newOptions.series = series;

        await GraphUtils.renderChartForResult(newOptions, charts, question, isExportPDF);
    };

    /**
     * Generate and render graph of the results of a cursor question
     * @param question    Question object which we want to display the results
     * @param charts      ApexChart to render at the end
     * @param responses   Array of responses which we want to display the results
     * @param isExportPDF Boolean to determine if we generate a graph for result or for PDF Export
     */
    private static generateCursorChart = async (question: Question, charts: ApexChart[], responses: Response[],
                                                isExportPDF: boolean) : Promise<void> => {
        // build array with all response
        let resp: number[] = responses.map((r: Response) => Number(r.answer)).sort((a: number, b: number) => a - b);
        let cursorAverage: string = (resp.reduce((a: number, b: number) => a + b, 0) / resp.length).toFixed(2);

        // map to build object with response and number of each one
        const map: Map<number, number> = resp.reduce((acc: Map<number, number>, e: number) =>
            acc.set(e, (acc.get(e) || 0) + 1), new Map());

        let labels: number[] = Array.from(map.keys());
        let colors: string[] = ColorUtils.generateColorList(labels.length);

        let newPDFOptions: any = isExportPDF ?
            GraphUtils.generateOptions(question.question_type, colors, labels,
            null, null, null, cursorAverage)
            :
            GraphUtils.generateOptions(question.question_type, colors, labels,
            '100%', '100%', null, cursorAverage);

        newPDFOptions.series = [{ name: lang.translate('formulaire.response.average'), data: Array.from(map.values()) }];

        await GraphUtils.renderChartForResult(newPDFOptions, charts, question, isExportPDF);
    }

    /**
     * Generate and render graph of the results of a multiple answers question
     * @param question    Question object which we want to display the results
     * @param charts      ApexCharts to store and render at the end
     * @param distribs    Distrib's number for each question
     * @param isExportPDF Boolean to identify if it's a PDF export case
     */
    static generateMultipleAnswerChart = async (question: Question, charts: ApexChart[], distribs: number, isExportPDF: boolean) : Promise<void> => {
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

        await GraphUtils.renderChartForResult(newOptions, charts, question, isExportPDF);
    }

    /**
     * Render graph with ApexChart based on given options
     * @param options     ApexChart options to render the graph
     * @param charts      ApexCharts to store and render at the end
     * @param question    Question object which we want to display the results
     * @param isExportPDF Boolean to identify if it's a PDF export case
     */
    static renderChartForResult = async (options: any, charts: any, question: Question, isExportPDF: boolean) : Promise<void> => {
        if (isExportPDF) {
            charts.push(new ApexCharts(document.querySelector(`#pdf-response-chart-${charts.length}`), options));
        } else {
            charts.push(new ApexCharts(document.querySelector(`#chart-${question.id}`), options));
        }
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
     * @param cursorAverage Average of answers
     */
    static generateOptions = (type: Types, colors: string[], labels: (string | number)[], height?: any, width?: any,
                              seriesPercent?: number[], cursorAverage?: string) : any => {
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
        else if (type === Types.CURSOR) {
            options = {
                chart: {
                    type: 'area',
                    height: height ? height : 400,
                    width: width ? width : 600,
                    animations: {
                        enabled: false
                    },
                    toolbar: {
                        show: false
                    },
                    zoom: {
                        enabled: false
                    }
                },
                colors: colors,
                dataLabels: {
                    enabled: false
                },
                xaxis: {
                    categories: labels,
                    title: {
                        text: lang.translate('formulaire.response.average') + ' (' + cursorAverage + ')'
                    }
                },
                yaxis: {
                    opposite: true,
                    labels: {
                        formatter: (value) => {
                            return Math.floor(value)
                        }
                    }
                },
                fill: {
                    type: "gradient",
                    gradient: {
                        shadeIntensity: 1,
                        opacityFrom: 0.7,
                        opacityTo: 0.9,
                        stops: [0, 90, 100]
                    }
                },
            }
        }
        return options;
    }
}