import {Constants} from "@common/core/constants";
import {UtilsUtils} from "@common/utils/utils";

export class ColorUtils {
    /**
     * Check if a given string is valid hexadecimal color
     * @param color String to test
     */
    static isHexaColor = (color: string) : boolean => {
        return /^#?[0-9A-F]{6}$/i.test(color);
    }

    /**
     * Check if a given string is valid RGB color
     * @param color String to test
     */
    static isRgbColor = (color: string) : boolean => {
        let isRgbFormat: boolean = /rgb\((\d{1,3}), ?(\d{1,3}), ?(\d{1,3})\)/.test(color);
        if (isRgbFormat) {
            let rgbValues: number[] = color.match(/\d+/g).map(Number);
            if (rgbValues.length < 3 ||rgbValues.length > 4) {
                return false;
            }
            else {
                for (let i: number = 0; i < 3; i++) {
                    let value: number = rgbValues[i];
                    if (value < 0 || value > 255) {
                        return false;
                    }
                }
                if (rgbValues.length === 4) {
                    let value: number = rgbValues[3];
                    if (value < 0 || value > 1) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Convert given RGB color into hexadecimal color
     * @param color RGB color to convert
     */
    static rgbToHexa = (color : string) : string => {
        if (ColorUtils.isHexaColor(color)) {
            return color;
        }
        else if (ColorUtils.isRgbColor(color)) {
            let rgbValues: number[] = color.match(/\d+/g).map(Number);
            // Convert each element to a base16 string and add zero if we get only one character
            let hexColor: string[] = rgbValues.map((rgbColor: number) => {
                let x16: string = rgbColor.toString(16);
                return x16.length === 1 ? "0" + x16 : x16;
            });
            return "#" + hexColor.join("");
        }
        return null;
    };

    /**
     * Convert given hexadecimal color into RGB color
     * @param color RGB color to convert
     */
    static hexaToRgb = (color : string) : string => {
        if (ColorUtils.isRgbColor(color)) {
            return color;
        }
        else if (ColorUtils.isHexaColor(color)) {
            let hexValues: RegExpExecArray = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(color);
            let rgbColor: number[] = hexValues ? [
                parseInt(hexValues[1], 16),
                parseInt(hexValues[2], 16),
                parseInt(hexValues[3], 16)
            ] : null;
            return "rgb(" + rgbColor.join(",") + ")";
        }
        return null;
    };

    /**
     * Returns values of an rgb color
     * @param color String RGB color
     */
    static getRgbValues = (color: string) : number[] => {
        let rgbColor: string = ColorUtils.hexaToRgb(color);
        return rgbColor.match(/\d+/g).map(Number);
    }


    /**
     * Returns a single rgb color interpolation between a list of given colors
     * @param paletteColors List of colors
     * @param factor        Factor of interpolation
     */
    static interpolateColor = (paletteColors: string[], factor: number = 0.5) : number[] => {
        factor = factor < 0 ? 0 : (factor > 1 ? 1 : factor);

        let positionOnGradient: number = (paletteColors.length - 1) * factor;
        let lowerValue: number = factor === 1 ? positionOnGradient - 1 : Math.floor(positionOnGradient);
        let upperValue: number = factor === 0 ? positionOnGradient + 1 : Math.ceil(positionOnGradient);
        let color1Values: number[] = ColorUtils.getRgbValues(paletteColors[lowerValue]);
        let color2Values: number[] = ColorUtils.getRgbValues(paletteColors[upperValue]);
        let newFactor: number = positionOnGradient / upperValue;
        let result: number[] = [];
        for (let i: number = 0; i < color1Values.length; i++) {
            result.push(Math.round(color1Values[i] + (color2Values[i] - color1Values[i]) * newFactor));
        }
        return result;
    };

    /**
     * Returns an array of colors interpolated between a list of given colors
     * @param paletteColors List of colors
     * @param nbColors      Number of colors wanted between the two given colors (included)
     */
    static interpolateColors = (paletteColors: string[], nbColors: number) : string[] => {
        if (nbColors <= 1) {
            return ["rgb(" + ColorUtils.interpolateColor(paletteColors).join(",") + ")"];
        }
        else {
            let stepFactor: number = 1 / (nbColors - 1 > 0 ? nbColors - 1 : 1);
            let interpolatedColorArray: string[] = [];

            for (let i: number = 0; i < nbColors; i++) {
                let rgbValues: number[] = ColorUtils.interpolateColor(paletteColors, stepFactor * i);
                let rgbString: string = "rgb(" + rgbValues.join(",") + ")";
                interpolatedColorArray.push(ColorUtils.rgbToHexa(rgbString));
            }

            return interpolatedColorArray;
        }
    }

    /**
     * Generate a list of colors randomly with the Constants colors lists
     * @param nbColors Number of colors we want to pick
     */
    static generateColorList = (nbColors: number) : string[] => {
        let colorsToDisplay: string[] = [Constants.BLUE_COLORS[Math.floor(Math.random() * Constants.BLUE_COLORS.length)]];
        let colorsToExclude: string[] = [colorsToDisplay[0]];
        let lastUsedGroup: string[] = Constants.BLUE_COLORS;

        while (colorsToDisplay.length < nbColors) {
            let newColorGroup: string[] = UtilsUtils.getRandomValueInList(Constants.COLORS_GROUPS, [lastUsedGroup]);
            let newColor: string = UtilsUtils.getRandomValueInList(newColorGroup, colorsToExclude);

            colorsToDisplay.push(newColor);
            colorsToExclude.push(newColor);
            if (colorsToExclude.length == Constants.NB_COLORS_AVAILABLE) colorsToExclude = [];
            lastUsedGroup = newColorGroup;
        }

        return colorsToDisplay;
    }
}