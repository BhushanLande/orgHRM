package utils;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.awt.image.BufferedImage;

public class OcrUtils {

    public static String ocr(BufferedImage img, String lang /* e.g., "eng" */) throws Exception {
        ITesseract t = new Tesseract();
        t.setLanguage(lang);
        t.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        return t.doOCR(img).trim();
    }
}
