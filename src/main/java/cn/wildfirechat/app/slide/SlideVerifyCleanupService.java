package cn.wildfirechat.app.slide;

import cn.wildfirechat.app.jpa.SlideVerifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlideVerifyCleanupService {

    @Autowired
    private SlideVerifyRepository slideVerifyRepository;

    @Transactional
    public void cleanupExpired() {
        slideVerifyRepository.deleteExpired(System.currentTimeMillis() - 300 * 1000);
    }
}
