package com.resumematch.pdf;

import java.io.IOException;

import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * Extrai o texto olhando como cada letra e desenhada.
 *
 * O PDFTextStripper padrao nao acompanha a cor; os operadores de cor sao
 * registrados aqui para que a cor de cada letra seja conhecida. Letra em modo
 * invisivel ou totalmente transparente e contada e, se pedido, deixada fora
 * do texto. Letra branca, minuscula ou fora da pagina so e contada.
 */
final class VisibilityTextStripper extends PDFTextStripper {

    private static final float TINY_FONT_PT = 4f;
    private static final double TRANSPARENT = 0.05;
    private static final int WHITE_COMPONENT = 242;
    private static final int SAMPLE_LIMIT = 160;

    private final boolean skipInvisible;
    private int visibleChars;
    private int invisibleChars;
    private int whiteChars;
    private int tinyChars;
    private int outsideChars;
    private final StringBuilder invisibleSample = new StringBuilder();
    private final StringBuilder suspiciousSample = new StringBuilder();
    private StringBuilder lastSample;

    VisibilityTextStripper(boolean skipInvisible) {
        this.skipInvisible = skipInvisible;
        addOperator(new SetNonStrokingColorSpace(this));
        addOperator(new SetNonStrokingColor(this));
        addOperator(new SetNonStrokingColorN(this));
        addOperator(new SetNonStrokingDeviceGrayColor(this));
        addOperator(new SetNonStrokingDeviceRGBColor(this));
        addOperator(new SetNonStrokingDeviceCMYKColor(this));
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        String unicode = text.getUnicode();
        if (unicode == null || unicode.isBlank()) {
            if (lastSample != null) {
                append(lastSample, " ");
            }
            super.processTextPosition(text);
            return;
        }
        PDGraphicsState state = getGraphicsState();
        RenderingMode mode = state.getTextState().getRenderingMode();
        boolean invisible = mode == RenderingMode.NEITHER || mode == RenderingMode.NEITHER_CLIP
                || state.getNonStrokeAlphaConstant() < TRANSPARENT;
        lastSample = null;
        if (invisible) {
            invisibleChars++;
            append(invisibleSample, unicode);
            lastSample = invisibleSample;
            if (skipInvisible) {
                return;
            }
        } else {
            visibleChars++;
            boolean suspicious = false;
            if (isWhite(state.getNonStrokingColor())) {
                whiteChars++;
                suspicious = true;
            }
            if (text.getFontSizeInPt() > 0 && text.getFontSizeInPt() < TINY_FONT_PT) {
                tinyChars++;
                suspicious = true;
            }
            if (outsidePage(text)) {
                outsideChars++;
                suspicious = true;
            }
            if (suspicious) {
                append(suspiciousSample, unicode);
                lastSample = suspiciousSample;
            }
        }
        super.processTextPosition(text);
    }

    int visibleChars() {
        return visibleChars;
    }

    int invisibleChars() {
        return invisibleChars;
    }

    HiddenTextReport report(boolean scannedLayer) {
        return new HiddenTextReport(invisibleChars, whiteChars, tinyChars, outsideChars, scannedLayer,
                invisibleSample.toString().strip(), suspiciousSample.toString().strip());
    }

    private static boolean isWhite(PDColor color) {
        if (color == null) {
            return false;
        }
        try {
            int rgb = color.toRGB();
            return ((rgb >> 16) & 0xFF) >= WHITE_COMPONENT
                    && ((rgb >> 8) & 0xFF) >= WHITE_COMPONENT
                    && (rgb & 0xFF) >= WHITE_COMPONENT;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static boolean outsidePage(TextPosition text) {
        float x = text.getXDirAdj();
        float y = text.getYDirAdj();
        return x < -1 || y < -1 || x > text.getPageWidth() + 1 || y > text.getPageHeight() + 1;
    }

    /** Guarda so o comeco: a amostra e para a pessoa reconhecer o trecho. */
    private static void append(StringBuilder sample, String unicode) {
        if (sample.length() < SAMPLE_LIMIT
                && !(unicode.equals(" ") && (sample.isEmpty() || sample.charAt(sample.length() - 1) == ' '))) {
            sample.append(unicode);
        }
    }
}
