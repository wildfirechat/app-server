package cn.wildfirechat.app.conference;

import cn.wildfirechat.app.jpa.ConferenceEntity;
import cn.wildfirechat.app.jpa.ConferenceEntityRepository;
import cn.wildfirechat.app.jpa.UserConferenceRepository;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.sdk.ConferenceAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConferenceCleanupService {
    private static final Logger LOG = LoggerFactory.getLogger(ConferenceCleanupService.class);

    @Autowired
    private ConferenceEntityRepository conferenceEntityRepository;

    @Autowired
    private ConferenceServiceImpl conferenceServiceImpl;

    @Autowired
    private UserConferenceRepository userConferenceRepository;

    /**
     * 每5分钟检查一次过期会议，并调用SDK销毁
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cleanupExpiredConferences() {
        long currentTime = System.currentTimeMillis() / 1000; // 转换为秒
        LOG.info("开始检查过期会议，当前时间: {} 秒", currentTime);

        List<ConferenceEntity> expiredConferences = conferenceEntityRepository.findExpiredConferences(currentTime);
        LOG.info("发现 {} 个过期会议", expiredConferences.size());

        for (ConferenceEntity conference : expiredConferences) {
            try {
                LOG.info("正在销毁过期会议: {}, endTime: {}", conference.id, conference.endTime);
                
                // 调用SDK销毁会议
                IMResult<Void> result = ConferenceAdmin.destroy(conference.id, conference.advance);
                if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    LOG.info("成功销毁会议: {}", conference.id);
                } else {
                    LOG.warn("销毁会议 {} 返回错误: {}", conference.id, 
                        result != null ? result.getErrorCode().getMsg() : "null result");
                }

                Thread.sleep(100);
                
                // 记录会议结束并更新使用量（使用计划的endTime作为实际结束时间）
                conferenceServiceImpl.endConferenceAndUpdateUsage(conference.id, conference.endTime);

                // 删除该会议的所有收藏记录
                userConferenceRepository.deleteByConferenceId(conference.id);
                LOG.info("已删除会议的收藏记录: {}", conference.id);
                
                // 从数据库删除会议记录
                conferenceEntityRepository.delete(conference);
                LOG.info("已从数据库删除会议记录: {}", conference.id);
                
            } catch (Exception e) {
                LOG.error("销毁会议 {} 时发生异常", conference.id, e);
            }
        }

        LOG.info("过期会议清理完成，共处理 {} 个会议", expiredConferences.size());
    }
}
