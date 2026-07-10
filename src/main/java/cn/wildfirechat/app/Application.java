package cn.wildfirechat.app;

import cn.wildfirechat.app.jpa.PCSessionRepository;
import cn.wildfirechat.app.jpa.ShiroSessionRepository;
import cn.wildfirechat.app.slide.SlideVerifyCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

@SpringBootApplication
@ServletComponentScan
@EnableScheduling
public class Application {
    @Autowired
    private PCSessionRepository pcSessionRepository;

    @Autowired
    private ShiroSessionRepository shiroSessionRepository;

    @Autowired
    private SlideVerifyCleanupService slideVerifyCleanupService;

	public static void main(String[] args) {
		System.setProperty("log4j.configurationFile", "config/log4j2.xml");
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

    /**
     * 统一调度线程池。避免多个 @Scheduled 任务（尤其是每秒一次的 PC 登录轮询）
     * 占用唯一默认线程，导致清理任务被长时间阻塞。
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("app-scheduler-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void clearPCSession(){
        pcSessionRepository.deleteByCreateDtBefore(System.currentTimeMillis() - 60 * 60 * 1000);
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void cleanExpiredSlideVerify(){
        slideVerifyCleanupService.cleanupExpired();
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void clearExpiredShiroSession(){
        // 清理 3 个月未更新的 shiro session；保留 update_time 为 0 的历史数据
        shiroSessionRepository.deleteExpiredSessions(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000);
    }

}
