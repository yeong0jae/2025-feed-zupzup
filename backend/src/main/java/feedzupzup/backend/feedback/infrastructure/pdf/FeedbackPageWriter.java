package feedzupzup.backend.feedback.infrastructure.pdf;

import feedzupzup.backend.feedback.domain.Feedback;
import feedzupzup.backend.feedback.domain.FeedbackDownloadJobStore;
import feedzupzup.backend.feedback.infrastructure.download.FeedbackWithImage;
import feedzupzup.backend.feedback.infrastructure.download.ImageDownloadResult;
import feedzupzup.backend.global.exception.InfrastructureException.PdfExportException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

@RequiredArgsConstructor
@Slf4j
public class FeedbackPageWriter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PDDocument document;
    private final PDType0Font font;
    private final BlockingQueue<FeedbackWithImage> queue;
    private final PdfLayoutConfig layout;
    private final FeedbackDownloadJobStore jobStore;
    private final String jobId;

    void consumeToPdf(final int totalCount) {
        int processedCount = 0;
        List<FeedbackWithImage> batch = new ArrayList<>(layout.feedbacksPerPage());

        try {
            while (true) {
                final FeedbackWithImage item = queue.take();

                if (item.isPoisonPill()) {
                    // 마지막 배치 처리
                    if (!batch.isEmpty()) {
                        writePage(batch, processedCount);
                    }
                    break;
                }

                batch.add(item);

                // 4개 모이거나 마지막이면 페이지 작성
                if (batch.size() == layout.feedbacksPerPage()) {
                    writePage(batch, processedCount);
                    batch.clear(); // 메모리 해제
                }

                processedCount++;

                if (processedCount % 10 == 0 || processedCount == totalCount) {
                    log.info("PDF 작성 진행: {}/{}", processedCount, totalCount);
                    updateProgress(processedCount, totalCount);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PdfExportException("PDF 파일 생성이 중단되었습니다.");
        } catch (IOException e) {
            throw new PdfExportException("PDF 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void writePage(final List<FeedbackWithImage> batch, final int startNum) throws IOException {
        final PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            for (int i = 0; i < batch.size(); i++) {
                final FeedbackWithImage item = batch.get(i);
                final int row = i / layout.feedbacksPerRow();
                final int col = i % layout.feedbacksPerRow();

                writeFeedback(contentStream, item, row, col, startNum + i + 1);
            }
        }

        // batch는 메서드 종료 후 clear되어 메모리 해제
    }

    private void writeFeedback(
            final PDPageContentStream contentStream,
            final FeedbackWithImage item,
            final int row,
            final int col,
            final int feedbackNum
    ) throws IOException {
        final Feedback feedback = item.feedback();
        final ImageDownloadResult imageResult = item.imageResult();

        // 피드백 영역의 시작 위치 계산
        final float x = layout.marginLeft() + (col * layout.feedbackWidth());
        final float y = layout.pageHeight() - layout.marginTop() - ((row + 1) * layout.feedbackHeight());

        float currentY = layout.pageHeight() - layout.marginTop() - (row * layout.feedbackHeight()) - 10;

        // 피드백 번호
        currentY = writeText(contentStream, x + 5, currentY, "번호: " + feedbackNum, layout.labelFontSize());

        // 내용
        currentY = writeText(contentStream, x + 5, currentY - 12,
                "내용: " + truncate(feedback.getContent().getValue(), 30), layout.valueFontSize());

        // 카테고리
        currentY = writeText(contentStream, x + 5, currentY - 10,
                "카테고리: " + feedback.getOrganizationCategory().getCategory().getKoreanName(),
                layout.valueFontSize());

        // 좋아요 수
        currentY = writeText(contentStream, x + 5, currentY - 10,
                "좋아요: " + feedback.getLikeCountValue(), layout.valueFontSize());

        // 비밀글 여부
        currentY = writeText(contentStream, x + 5, currentY - 10,
                "비밀글: " + (feedback.isSecret() ? "Y" : "N"), layout.valueFontSize());

        // 상태
        currentY = writeText(contentStream, x + 5, currentY - 10,
                "상태: " + feedback.getStatus().name(), layout.valueFontSize());

        // 작성자
        currentY = writeText(contentStream, x + 5, currentY - 10,
                "작성자: " + feedback.getUserName().getValue(), layout.valueFontSize());

        // 작성일
        currentY = writeText(contentStream, x + 5, currentY - 10,
                "작성일: " + feedback.getPostedAt().getValue().format(DATE_FORMATTER), layout.valueFontSize());

        // 이미지 처리
        currentY -= 15;
        writeImage(contentStream, imageResult, x + 5, currentY);
    }

    private float writeText(
            final PDPageContentStream contentStream,
            final float x,
            final float y,
            final String text,
            final float fontSize
    ) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y;
    }

    private void writeImage(
            final PDPageContentStream contentStream,
            final ImageDownloadResult imageResult,
            final float x,
            final float y
    ) throws IOException {
        if (imageResult.isNoImage()) {
            writeText(contentStream, x, y - 10, "(이미지 없음)", layout.valueFontSize());
            return;
        }

        if (imageResult.isFailed()) {
            writeText(contentStream, x, y - 10, "(이미지 로드 실패)", layout.valueFontSize());
            return;
        }

        try {
            // 이미지를 PDDocument에 임베딩 (MemoryUsageSetting에 의해 디스크로 스트리밍)
            final PDImageXObject image = PDImageXObject.createFromByteArray(
                    document,
                    imageResult.imageData(),
                    "image"
            );

            // 이미지 크기 계산 (비율 유지하며 최대 크기 내로 축소)
            final float imageWidth = image.getWidth();
            final float imageHeight = image.getHeight();
            final float aspectRatio = imageHeight / imageWidth;

            float drawWidth = Math.min(imageWidth, layout.imageMaxWidth());
            float drawHeight = drawWidth * aspectRatio;

            if (drawHeight > layout.imageMaxHeight()) {
                drawHeight = layout.imageMaxHeight();
                drawWidth = drawHeight / aspectRatio;
            }

            // 이미지 그리기
            contentStream.drawImage(image, x, y - drawHeight, drawWidth, drawHeight);

            // byte[] 참조는 이 메서드 종료 후 GC 대상
        } catch (Exception e) {
            log.error("이미지 삽입 실패", e);
            writeText(contentStream, x, y - 10, "(이미지 삽입 실패)", layout.valueFontSize());
        }
    }

    private String truncate(final String text, final int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private void updateProgress(final int processedCount, final int totalCount) {
        if (jobStore != null && jobId != null) {
            jobStore.updateProgress(jobId, processedCount, totalCount);
        }
    }
}
