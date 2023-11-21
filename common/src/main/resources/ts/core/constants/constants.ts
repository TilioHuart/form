export abstract class Constants {
    static readonly DEFAULT_DOMAIN: string = "default-domain";
    static readonly DEFAULT_NB_CHOICES: number = 3;
    static readonly DEFAULT_NB_CHILDREN: number = 3;
    static readonly DEFAULT_CURSOR_MIN_VALUE: number = 1;
    static readonly DEFAULT_CURSOR_MAX_VALUE: number = 10;
    static readonly DEFAULT_CURSOR_STEP: number = 1;
    static readonly MAX_FILES_SAVE: number = 10;

    // Colors
    static readonly GRAPH_COLORS: string[] = ['#37A4CD','#1691C0','#056F98','#5AB7DA','#89CEE9'];
    static readonly BLUE_COLORS: string[] = ['#37A4CD','#1691C0','#056F98','#5AB7DA','#89CEE9'];
    static readonly YELLOW_COLORS: string[] = ['#FFC73C','#F2AE00','#FFBB13','#FFD263','#FFDF91'];
    static readonly PURPLE_COLORS: string[] = ['#475DD5','#1129A6','#2741C9','#687BE0','#93A1EC'];
    static readonly ORANGE_COLORS: string[] = ['#FFA23C','#F27E00','#FF8E13','#FFB463','#FFCA91'];
    static readonly COLORS_GROUPS: Array<string[]> = [Constants.BLUE_COLORS, Constants.YELLOW_COLORS, Constants.PURPLE_COLORS, Constants.ORANGE_COLORS];
    static readonly NB_COLORS_AVAILABLE: number = 25;

    // Dates format
    static readonly HH_MM: string = "HH:mm";
    static readonly DD_MM_YYYY: string = "DD/MM/YYYY";

    // Types
    static readonly STRING: string = "string";
    static readonly FILE: string = "file";

}