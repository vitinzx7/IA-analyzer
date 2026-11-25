package com.resumematch.pdf;

/**
 * O que a leitura do PDF achou de texto que uma pessoa nao ve.
 *
 * Contagens em caracteres sem espaco.
 *
 * @param invisibleChars texto em modo invisivel ou totalmente transparente;
 *        ficou fora da analise
 * @param whiteChars texto branco; entrou na analise, pode ser legitimo (letra
 *        branca sobre fundo escuro)
 * @param tinyChars texto com fonte menor que 4 pt; entrou na analise
 * @param outsideChars texto posicionado fora da pagina; entrou na analise
 * @param scannedLayer quase todo o texto e invisivel: e a camada de texto de
 *        um PDF escaneado, e nada foi tirado
 * @param invisibleSample comeco do texto que ficou de fora
 * @param suspiciousSample comeco do texto branco, minusculo ou fora da pagina
 */
public record HiddenTextReport(int invisibleChars, int whiteChars, int tinyChars, int outsideChars,
        boolean scannedLayer, String invisibleSample, String suspiciousSample) {

    /** Abaixo disso, o achado e tratado como ruido do PDF e nao vira aviso. */
    public static final int MIN_REPORTED = 10;

    public static final HiddenTextReport CLEAN = new HiddenTextReport(0, 0, 0, 0, false, "", "");

    public boolean removedHiddenText() {
        return !scannedLayer && invisibleChars >= MIN_REPORTED;
    }

    public boolean hasSuspiciousText() {
        return whiteChars >= MIN_REPORTED || tinyChars >= MIN_REPORTED || outsideChars >= MIN_REPORTED;
    }
}
