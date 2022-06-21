package fr.openent.formulaire_public.helpers;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class CaptchaHelper {
    private static final Logger log = LoggerFactory.getLogger(CaptchaHelper.class);
    private static final List<String> numbers = Arrays.asList("zero", "un", "deux", "trois", "quatre", "cinq", "six", "sept",
            "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze","quinze", "seize", "dix-sept", "dix_huit", "dix-neuf", "vingt");

    private CaptchaHelper() {}

    public static String formatCaptchaAnswer(String captchaAnswer) {
        if (captchaAnswer == null) {
            return null;
        }

        try {
            int numberCaptchaAnswer = Integer.parseInt(captchaAnswer);
            if (numberCaptchaAnswer < 0 || numberCaptchaAnswer >= numbers.size()) {
                return captchaAnswer;
            }
            else {
                return numbers.get(numberCaptchaAnswer);
            }
        } catch (NumberFormatException nfe) {
            return captchaAnswer.toLowerCase();
        }
    }
}
