package feedzupzup.backend.feedback.infrastructure.download;

public record ImageDownloadResult(byte[] imageData, ResultType type) {

    enum ResultType {
        SUCCESS, FAILED, NO_IMAGE
    }

    static ImageDownloadResult success(final byte[] imageData) {
        return new ImageDownloadResult(imageData, ResultType.SUCCESS);
    }

    static ImageDownloadResult failed() {
        return new ImageDownloadResult(null, ResultType.FAILED);
    }

    static ImageDownloadResult noImage() {
        return new ImageDownloadResult(null, ResultType.NO_IMAGE);
    }

    public boolean isFailed() {
        return type == ResultType.FAILED;
    }

    public boolean isNoImage() {
        return type == ResultType.NO_IMAGE;
    }

    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }
}
