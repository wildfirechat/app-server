package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.PCSessionRepository;
import cn.wildfirechat.app.jpa.SlideVerifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

@SpringBootApplication
@ServletComponentScan
@EnableScheduling
public class Application {
    @Autowired
    private PCSessionRepository pcSessionRepository;

    @Autowired
    private SlideVerifyRepository slideVerifyRepository;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}


	/**
	 * 文件上传配置
	 * @return
	 */
	@Bean
	public MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		//单个文件最大
		factory.setMaxFileSize(DataSize.ofMegabytes(20)); //20MB
		/// 设置总上传数据总大小
		factory.setMaxRequestSize(DataSize.ofMegabytes(100));
		return factory.createMultipartConfig();
	}

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void clearPCSession(){
        pcSessionRepository.deleteByCreateDtBefore(System.currentTimeMillis() - 60 * 60 * 1000);
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanExpiredSlideVerify(){
        slideVerifyRepository.deleteExpired(java.time.Instant.now().minusSeconds(300));
    }

}
