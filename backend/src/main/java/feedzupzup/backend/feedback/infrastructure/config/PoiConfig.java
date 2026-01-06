package feedzupzup.backend.feedback.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.ZipPackage;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class PoiConfig {

    @PostConstruct
    public void init() {
        ZipPackage.setUseTempFilePackageParts(true);
        log.info("POI ZipPackage 임시 파일 사용 모드로 설정 완료");
    }
}
