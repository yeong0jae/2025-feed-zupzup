package feedzupzup.backend.feedback.infrastructure.pdf;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

public record PdfLayoutConfig(
        float pageWidth,
        float pageHeight,
        float marginTop,
        float marginBottom,
        float marginLeft,
        float marginRight,
        float feedbackWidth,
        float feedbackHeight,
        int feedbacksPerRow,
        int feedbacksPerPage,
        float labelFontSize,
        float valueFontSize,
        float imageMaxWidth,
        float imageMaxHeight
) {

    public static PdfLayoutConfig createDefault() {
        final PDRectangle a4 = PDRectangle.A4;
        final float pageWidth = a4.getWidth();
        final float pageHeight = a4.getHeight();

        // 여백 설정
        final float marginTop = 40;
        final float marginBottom = 40;
        final float marginLeft = 40;
        final float marginRight = 40;

        // 2x2 그리드 설정
        final int feedbacksPerRow = 2;
        final int feedbacksPerPage = 4;

        // 피드백 1개당 영역 크기 계산
        final float availableWidth = pageWidth - marginLeft - marginRight;
        final float availableHeight = pageHeight - marginTop - marginBottom;
        final float feedbackWidth = availableWidth / 2;  // 2열
        final float feedbackHeight = availableHeight / 2;  // 2행

        // 폰트 크기
        final float labelFontSize = 9;
        final float valueFontSize = 8;

        // 이미지 최대 크기 (피드백 영역 내 여백 고려)
        final float imageMaxWidth = feedbackWidth - 20;
        final float imageMaxHeight = feedbackHeight - 80;  // 텍스트 영역 제외

        return new PdfLayoutConfig(
                pageWidth,
                pageHeight,
                marginTop,
                marginBottom,
                marginLeft,
                marginRight,
                feedbackWidth,
                feedbackHeight,
                feedbacksPerRow,
                feedbacksPerPage,
                labelFontSize,
                valueFontSize,
                imageMaxWidth,
                imageMaxHeight
        );
    }
}
