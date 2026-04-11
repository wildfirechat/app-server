package cn.wildfirechat.app.conference;


import cn.wildfirechat.app.IMConfig;
import cn.wildfirechat.app.RestResult;
import cn.wildfirechat.app.jpa.*;
import cn.wildfirechat.app.model.ConferenceDTO;
import cn.wildfirechat.app.pojo.*;
import cn.wildfirechat.app.tools.NumericIdGenerator;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.PojoConferenceInfo;
import cn.wildfirechat.pojos.PojoConferenceInfoList;
import cn.wildfirechat.sdk.*;
import cn.wildfirechat.sdk.model.IMResult;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ConferenceServiceImpl implements ConferenceService {
    private static final Logger LOG = LoggerFactory.getLogger(ConferenceServiceImpl.class);

    @Autowired
    private IMConfig mIMConfig;

    @Autowired
    private ConferenceEntityRepository conferenceEntityRepository;

    @Autowired
    private UserPrivateConferenceIdRepository userPrivateConferenceIdRepository;

    @Autowired
    private UserConferenceRepository userConferenceRepository;

    @Autowired
    private UserConferenceQuotaRepository userConferenceQuotaRepository;

    @Autowired
    private UserQuotaUsageRepository userQuotaUsageRepository;

    @Autowired
    private ConferenceRecordRepository conferenceRecordRepository;

    @Value("${conference.default_quota_minutes:0}")
    private int defaultQuotaMinutes;

    @PostConstruct
    private void init() {
        AdminConfig.initAdmin(mIMConfig.admin_url, mIMConfig.admin_secret);
    }

    @Override
    public RestResult getUserConferenceId(String userId) {
        String conferenceId = getPrivateConferenceId(userId);
        return RestResult.ok(conferenceId);
    }

    @Override
    public RestResult getMyConferenceId() {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        return getUserConferenceId(userId);
    }

    private String getPrivateConferenceId(String userId) {
        Optional<UserPrivateConferenceId> privateConferenceIdOptional = userPrivateConferenceIdRepository.findById(userId);
        if(privateConferenceIdOptional.isPresent()) {
            return privateConferenceIdOptional.get().getConferenceId();
        }
        String conferenceId = NumericIdGenerator.getId(null, Arrays.asList(0), 8);
        userPrivateConferenceIdRepository.save(new UserPrivateConferenceId(userId, conferenceId));

        return conferenceId;
    }

    @Override
    public RestResult getConferenceInfo(String conferenceId, String password) {
        Optional<ConferenceEntity> conferenceEntityOptional = conferenceEntityRepository.findById(conferenceId);
        if(conferenceEntityOptional.isPresent()) {
            Subject subject = SecurityUtils.getSubject();
            String userId = (String) subject.getSession().getAttribute("userId");
            ConferenceEntity entity = conferenceEntityOptional.get();
            if(StringUtils.isEmpty(entity.password) || entity.password.equals(password) || userId.equals(entity.owner)) {
                return RestResult.ok(convertConference(entity));
            }
        }

        return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
    }

    @Override
    public RestResult putConferenceInfo(ConferenceInfo info) {
        Optional<ConferenceEntity> conferenceEntityOptional = conferenceEntityRepository.findById(info.conferenceId);
        if(conferenceEntityOptional.isPresent()) {
            Subject subject = SecurityUtils.getSubject();
            String userId = (String) subject.getSession().getAttribute("userId");
            ConferenceEntity entity = conferenceEntityOptional.get();
            if(userId.equals(entity.owner)) {
                conferenceEntityRepository.save(convertConference(info));
            } else {
                return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
            }
        } else {
            conferenceEntityRepository.save(convertConference(info));
        }

        return RestResult.ok(null);
    }

    @Override
    public RestResult createConference(ConferenceInfo info) {
        String userId = getUserId();
        if(!StringUtils.isEmpty(info.owner)) {
            if(!info.owner.equals(userId)) {
                return RestResult.error(RestResult.RestCode.ERROR_INVALID_PARAMETER);
            }
        } else {
            info.owner = userId;
        }

        //如果开始时间小于当前时间，改成当前时间
        if(info.startTime < System.currentTimeMillis()/1000) {
            info.startTime = System.currentTimeMillis()/1000;
        }

        //如果没有指定最大参与者人数，默认指定为20
        if(info.maxParticipants <= 0) {
            info.maxParticipants = 20;
        }

        // 检查配额（仅当会议有结束时间时）
        if (info.endTime > 0) {
            // 计算计划时长，calculateDurationMinutes 内部会处理开始时间
            int plannedMinutes = calculateDurationMinutes(info.startTime, info.endTime);
            LOG.info("用户 {} 创建会议，计划时长 {} 分钟（原始开始时间: {}, 结束时间: {}）", 
                userId, plannedMinutes, info.startTime, info.endTime);
            QuotaCheckResult checkResult = checkUserQuota(userId, plannedMinutes);
            if (!checkResult.isEnough()) {
                LOG.warn("用户 {} 会议额度不足，需要 {} 分钟，剩余 {} 分钟", 
                    userId, plannedMinutes, checkResult.getRemaining());
                return RestResult.error(RestResult.RestCode.ERROR_CONFERENCE_QUOTA_EXCEEDED);
            }
            LOG.info("用户 {} 配额检查通过，计划使用 {} 分钟，剩余 {} 分钟", 
                userId, plannedMinutes, checkResult.getRemaining());
        } else {
            LOG.info("用户 {} 创建永久会议（无结束时间），跳过配额检查", userId);
        }

        if(StringUtils.isEmpty(info.conferenceId)) {
            /*
            没有传来会议ID，这里生成随机会议ID。个人会议的长度是8位，随机会议ID是10位
             */
            String conferenceId = null;
            do {
                conferenceId = NumericIdGenerator.getId(null, Arrays.asList(0), 10);
                if(!conferenceEntityRepository.findById(conferenceId).isPresent()) {
                    break;
                }
            } while (true);
            info.conferenceId = conferenceId;
        } else {
            Optional<ConferenceEntity> conferenceEntityOptional = conferenceEntityRepository.findById(info.conferenceId);
            if(conferenceEntityOptional.isPresent()) {
                ConferenceEntity entity = conferenceEntityOptional.get();
                if(!userId.equals(entity.owner)) {
                    return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
                }
            }
        }

        try {
            IMResult<Void> result = ConferenceAdmin.createRoom(info.conferenceId, info.conferenceTitle, info.pin, info.maxParticipants, info.advance, 0, info.recording, false);
            if(result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                conferenceEntityRepository.save(convertConference(info));
                LOG.info("会议创建成功: conferenceId={}, owner={}, title={}", 
                    info.conferenceId, userId, info.conferenceTitle);
                
                // 创建会议记录（仅当会议有结束时间时）
                if (info.endTime > 0) {
                    createConferenceRecord(info);
                }
                
                favConference(info.conferenceId);
                return RestResult.ok(info.conferenceId);
            } else {
                LOG.error("创建会议失败: conferenceId={}, errorCode={}, errorMsg={}", 
                    info.conferenceId, 
                    result != null ? result.getErrorCode().code : "null",
                    result != null ? result.getErrorCode().msg : "null result");
            }
        } catch (Exception e) {
            LOG.error("创建会议异常: conferenceId={}", info.conferenceId, e);
        }
        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
    }

    @Override
    @Transactional
    public RestResult destroyConference(String conferenceId) {
        String userId = getUserId();
        LOG.info("用户 {} 请求销毁会议: {}", userId, conferenceId);
        Optional<ConferenceEntity> conferenceEntityOptional = conferenceEntityRepository.findById(conferenceId);
        if(conferenceEntityOptional.isPresent()) {
            ConferenceEntity entity = conferenceEntityOptional.get();
            if(!userId.equals(entity.owner)) {
                LOG.warn("用户 {} 无权销毁会议 {}，会议所有者是 {}", userId, conferenceId, entity.owner);
                return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
            }

            try {
                ConferenceAdmin.destroy(entity.id, entity.advance);
                LOG.info("SDK销毁会议成功: conferenceId={}", conferenceId);
            } catch (Exception e) {
                LOG.error("SDK销毁会议失败: conferenceId={}", conferenceId, e);
            }
            
            long actualEndTime = System.currentTimeMillis() / 1000;
            // 记录会议结束，更新使用时长
            endConferenceAndUpdateUsage(conferenceId, actualEndTime);

            // 删除该会议的所有收藏记录
            userConferenceRepository.deleteByConferenceId(conferenceId);
            LOG.info("已删除会议的收藏记录: conferenceId={}", conferenceId);
            
            conferenceEntityRepository.deleteById(conferenceId);
            LOG.info("会议已从数据库删除: conferenceId={}", conferenceId);
        } else {
            LOG.warn("销毁会议时未找到会议记录: conferenceId={}", conferenceId);
            try {
                IMResult<PojoConferenceInfoList> conferenceInfoListIMResult = ConferenceAdmin.listConferences(1000, 0);
                if(conferenceInfoListIMResult != null && conferenceInfoListIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                    for (PojoConferenceInfo info : conferenceInfoListIMResult.getResult().conferenceInfoList) {
                        if(info.roomId.equals(conferenceId)) {
                            ConferenceAdmin.destroy(info.roomId, info.advance);
                            LOG.info("通过列表找到并销毁会议: conferenceId={}", conferenceId);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("销毁会议异常: conferenceId={}", conferenceId, e);
            }
        }
        return RestResult.ok(null);
    }

    @Override
    public RestResult recordingConference(String conferenceId, boolean recording) {
        Optional<ConferenceEntity> conferenceEntityOptional = conferenceEntityRepository.findById(conferenceId);
        if(conferenceEntityOptional.isPresent()) {
            Subject subject = SecurityUtils.getSubject();
            String userId = (String) subject.getSession().getAttribute("userId");
            ConferenceEntity entity = conferenceEntityOptional.get();
            if(userId.equals(entity.owner)) {
                if(entity.isRecording() == recording) {
                    return RestResult.ok();
                } else {
                    entity.setRecording(recording);
                    try {
                        IMResult<Void> voidIMResult = ConferenceAdmin.enableRecording(entity.getId(), entity.isAdvance(), entity.recording);
                        if(voidIMResult != null & voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                            conferenceEntityRepository.save(entity);
                            return RestResult.ok();
                        } else {
                            return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
                    }
                }
            } else {
                return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
            }
        } else {
            return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
        }
    }

    @Override
    public RestResult focusConference(String conferenceId, String focusedUserId) {
        Optional<ConferenceEntity> conferenceEntityOptional = conferenceEntityRepository.findById(conferenceId);
        if(conferenceEntityOptional.isPresent()) {
            Subject subject = SecurityUtils.getSubject();
            String userId = (String) subject.getSession().getAttribute("userId");
            ConferenceEntity entity = conferenceEntityOptional.get();
            if(userId.equals(entity.owner)) {
                entity.setFocus(focusedUserId);
                conferenceEntityRepository.save(entity);
            } else {
                return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
            }
        } else {
            return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
        }
        return RestResult.error(RestResult.RestCode.SUCCESS);
    }

    @Override
    public RestResult favConference(String conferenceId) {
        String userId = getUserId();
        UserConference userConference = new UserConference(userId, conferenceId);
        userConferenceRepository.save(userConference);
        return RestResult.ok(null);
    }

    @Override
    public RestResult unfavConference(String conferenceId) {
        userConferenceRepository.deleteByUserIdAndConferenceId(getUserId(), conferenceId);
        return RestResult.ok(null);
    }

    @Override
    public RestResult getFavConferences() {
        List<ConferenceDTO> ucs = userConferenceRepository.findByUserId(getUserId(), System.currentTimeMillis()/1000);
        List<ConferenceInfo> infos = new ArrayList<>();
        for (ConferenceDTO dto:ucs) {
            ConferenceInfo info = new ConferenceInfo();
            info.conferenceId = dto.getId();
            info.conferenceTitle = dto.getConference_title();
            info.password = dto.getPassword();
            info.pin = dto.getPin();
            info.owner = dto.getOwner();
            info.startTime = dto.getStart_time();
            info.endTime = dto.getEnd_time();
            info.audience = dto.isAudience();
            info.advance = dto.isAdvance();
            info.allowSwitchMode = dto.isAllow_switch_mode();
            info.noJoinBeforeStart = dto.isNo_join_before_start();
            info.recording = dto.isRecording();
            info.focus = dto.getFocus();
            info.maxParticipants = dto.getMax_participants();
            String managers = dto.getManages();
            if(!StringUtils.isEmpty(managers)) {
                info.managers = Arrays.asList(managers.split(","));
            }
            infos.add(info);
        }
        return RestResult.ok(infos);
    }

    @Override
    public RestResult isFavConference(String conferenceId) {
        Optional<UserConference> userConference = userConferenceRepository.findByUserIdAndConferenceId(getUserId(), conferenceId);
        if(userConference.isPresent())
            return RestResult.ok(null);

        return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
    }

    @Override
    public RestResult getMyConferenceQuota() {
        String userId = getUserId();
        String currentYearMonth = getCurrentYearMonth();
        LOG.info("用户 {} 查询会议额度，年月: {}", userId, currentYearMonth);
        
        ConferenceQuotaResponse response = new ConferenceQuotaResponse();
        response.setYearMonth(currentYearMonth);
        
        // 获取用户配额
        Optional<UserConferenceQuota> quotaOptional = userConferenceQuotaRepository.findByUserId(userId);
        int totalQuota;
        String quotaSource;
        if (quotaOptional.isPresent()) {
            totalQuota = quotaOptional.get().getTotalMinutes();
            quotaSource = "用户自定义配额";
        } else {
            totalQuota = defaultQuotaMinutes;
            quotaSource = "默认配额";
        }
        
        response.setTotalQuota(totalQuota);
        
        // 配额为0表示不限制
        if (totalQuota == 0) {
            response.setUnlimited(true);
            response.setUsedMinutes(0);
            response.setRemainingMinutes(0);
            LOG.info("用户 {} 查询额度结果: 来源={}, 无限制", userId, quotaSource);
        } else {
            response.setUnlimited(false);
            
            // 查询当月已使用额度
            Optional<UserQuotaUsage> usageOptional = userQuotaUsageRepository.findByUserIdAndYearMonth(userId, currentYearMonth);
            int usedMinutes = usageOptional.map(UserQuotaUsage::getUsedMinutes).orElse(0);
            int remaining = totalQuota - usedMinutes;
            
            response.setUsedMinutes(usedMinutes);
            response.setRemainingMinutes(remaining);
            
            LOG.info("用户 {} 查询额度结果: 来源={}, 总额度={}分钟, 已使用={}分钟, 剩余={}分钟", 
                userId, quotaSource, totalQuota, usedMinutes, remaining);
        }
        
        return RestResult.ok(response);
    }

    /**
     * 检查用户配额是否充足
     * 配额不分月份，但使用量按月统计
     */
    private QuotaCheckResult checkUserQuota(String userId, int needMinutes) {
        // 获取用户配额（优先查用户自定义配额，没有则使用默认配置）
        Optional<UserConferenceQuota> quotaOptional = userConferenceQuotaRepository.findByUserId(userId);
        
        int totalQuota;
        String quotaSource;
        if (quotaOptional.isPresent()) {
            totalQuota = quotaOptional.get().getTotalMinutes();
            quotaSource = "用户自定义配额";
        } else {
            totalQuota = defaultQuotaMinutes;
            quotaSource = "默认配额";
        }
        
        LOG.debug("用户 {} 配额检查: 来源={}, 总额度={}分钟, 需要={}分钟", 
            userId, quotaSource, totalQuota, needMinutes);
        
        // 默认配额为0表示不限制
        if (totalQuota == 0) {
            LOG.debug("用户 {} 使用默认配额0，不限制会议时长", userId);
            return new QuotaCheckResult(true, 0, 0);
        }
        
        // 查询当月已使用额度
        String currentYearMonth = getCurrentYearMonth();
        Optional<UserQuotaUsage> usageOptional = userQuotaUsageRepository.findByUserIdAndYearMonth(userId, currentYearMonth);
        int usedMinutes = usageOptional.map(UserQuotaUsage::getUsedMinutes).orElse(0);
        
        // 检查是否足够
        boolean enough = (usedMinutes + needMinutes) <= totalQuota;
        int remaining = totalQuota - usedMinutes;
        
        LOG.debug("用户 {} 配额详情: 年月={}, 总额度={}, 已使用={}, 需要={}, 剩余={}, 结果={}", 
            userId, currentYearMonth, totalQuota, usedMinutes, needMinutes, remaining, 
            enough ? "通过" : "不足");
        
        return new QuotaCheckResult(enough, remaining, totalQuota);
    }

    /**
     * 创建会议记录
     */
    private void createConferenceRecord(ConferenceInfo info) {
        try {
            ConferenceRecord record = new ConferenceRecord();
            record.setConferenceId(info.conferenceId);
            record.setOwner(info.owner);

            long actualStartTime = info.startTime;
            record.setStartTime(actualStartTime);
            record.setEndTime(info.endTime);
            // 使用 actualStartTime 计算计划时长，确保与配额检查时一致
            record.setPlannedDuration(calculateDurationMinutes(actualStartTime, info.endTime));
            record.setActualDuration(0);
            record.setStatus(ConferenceRecord.Status.ONGOING.getValue());
            record.setYearMonth(getCurrentYearMonth());
            
            conferenceRecordRepository.save(record);
            LOG.info("创建会议记录: conferenceId={}, owner={}, startTime={}, plannedDuration={}分钟", 
                info.conferenceId, info.owner, actualStartTime, record.getPlannedDuration());
        } catch (Exception e) {
            LOG.error("创建会议记录失败: conferenceId={}", info.conferenceId, e);
        }
    }

    /**
     * 结束会议并更新使用量
     */
    @Transactional
    public void endConferenceAndUpdateUsage(String conferenceId, long actualEndTime) {
        try {
            Optional<ConferenceRecord> recordOptional = conferenceRecordRepository.findByConferenceId(conferenceId);
            if (!recordOptional.isPresent()) {
                LOG.warn("未找到会议记录: conferenceId={}", conferenceId);
                return;
            }
            
            ConferenceRecord record = recordOptional.get();
            if (record.getStatus() == ConferenceRecord.Status.ENDED.getValue()) {
                LOG.warn("会议已结束，跳过重复处理: conferenceId={}", conferenceId);
                return;
            }
            
            // 如果开始时间为0，使用当前时间作为开始时间（兼容老数据）
            long startTime = record.getStartTime();
            if (startTime <= 0) {
                startTime = actualEndTime; // 如果开始时间为0，结束时间作为开始时间，时长为0
                LOG.warn("会议记录开始时间为0，使用结束时间作为开始时间: conferenceId={}", conferenceId);
            }
            
            // 计算实际时长（分钟）
            int actualDuration = calculateDurationMinutes(startTime, actualEndTime);
            
            // 更新会议记录
            record.setActualDuration(actualDuration);
            record.setEndTime(actualEndTime);
            record.setStatus(ConferenceRecord.Status.ENDED.getValue());
            conferenceRecordRepository.save(record);
            
            // 更新用户使用量
            updateQuotaUsage(record.getOwner(), record.getYearMonth(), actualDuration);
            
            LOG.info("会议结束并更新使用量: conferenceId={}, owner={}, startTime={}, endTime={}, actualDuration={}分钟", 
                conferenceId, record.getOwner(), startTime, actualEndTime, actualDuration);
                
        } catch (Exception e) {
            LOG.error("结束会议并更新使用量失败: conferenceId={}", conferenceId, e);
        }
    }

    /**
     * 更新用户配额使用量
     */
    @Transactional
    public void updateQuotaUsage(String userId, String yearMonth, int minutes) {
        if (minutes <= 0) {
            LOG.debug("更新使用量跳过: 用户={}, 时长={} 分钟（小于等于0）", userId, minutes);
            return;
        }
        
        Optional<UserQuotaUsage> usageOptional = userQuotaUsageRepository.findByUserIdAndYearMonth(userId, yearMonth);
        if (usageOptional.isPresent()) {
            UserQuotaUsage usage = usageOptional.get();
            int oldMinutes = usage.getUsedMinutes();
            usage.setUsedMinutes(oldMinutes + minutes);
            userQuotaUsageRepository.save(usage);
            LOG.info("更新用户使用量: 用户={}, 年月={}, 新增 {} 分钟, 原使用 {} 分钟, 现使用 {} 分钟", 
                userId, yearMonth, minutes, oldMinutes, usage.getUsedMinutes());
        } else {
            UserQuotaUsage usage = new UserQuotaUsage(userId, yearMonth, minutes);
            userQuotaUsageRepository.save(usage);
            LOG.info("创建用户使用量记录: 用户={}, 年月={}, 使用 {} 分钟", 
                userId, yearMonth, minutes);
        }
    }

    /**
     * 计算时长（分钟）
     */
    private int calculateDurationMinutes(long startTime, long endTime) {
        long durationSeconds = endTime - startTime;
        if (durationSeconds <= 0) {
            return 0;
        }
        // 转换为分钟，向上取整
        return (int) ((durationSeconds + 59) / 60);
    }

    /**
     * 获取当前年月 (yyyyMM格式)
     */
    private String getCurrentYearMonth() {
        return Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private ConferenceEntity convertConference(ConferenceInfo info) {
        ConferenceEntity entity = new ConferenceEntity();
        entity.id = info.conferenceId;
        entity.conferenceTitle = info.conferenceTitle;
        entity.password = info.password;
        entity.pin = info.pin;
        entity.owner = info.owner;
        entity.startTime = info.startTime;
        entity.endTime = info.endTime;
        entity.audience = info.audience;
        entity.advance = info.advance;
        entity.allowSwitchMode = info.allowSwitchMode;
        entity.noJoinBeforeStart = info.noJoinBeforeStart;
        entity.recording = info.recording;
        entity.focus = info.focus;
        entity.maxParticipants = info.maxParticipants;
        if(info.managers != null && !info.managers.isEmpty()) {
            entity.manages = String.join(",", info.managers);
        }
        return entity;
    }

    private ConferenceInfo convertConference(ConferenceEntity entity) {
        ConferenceInfo info = new ConferenceInfo();
        info.conferenceId = entity.id;
        info.conferenceTitle = entity.conferenceTitle;
        info.password = entity.password;
        info.pin = entity.pin;
        info.owner = entity.owner;
        info.startTime = entity.startTime;
        info.endTime = entity.endTime;
        info.audience = entity.audience;
        info.advance = entity.advance;
        info.allowSwitchMode = entity.allowSwitchMode;
        info.noJoinBeforeStart = entity.noJoinBeforeStart;
        info.recording = entity.recording;
        info.focus = entity.focus;
        info.maxParticipants = entity.maxParticipants;
        if(!StringUtils.isEmpty(info.managers)) {
            info.managers = Arrays.asList(entity.manages.split(","));
        }
        return info;
    }

    private String getUserId() {
        Subject subject = SecurityUtils.getSubject();
        return (String) subject.getSession().getAttribute("userId");
    }

    /**
     * 配额检查结果
     */
    private static class QuotaCheckResult {
        private final boolean enough;
        private final int remaining;
        private final int total;

        public QuotaCheckResult(boolean enough, int remaining, int total) {
            this.enough = enough;
            this.remaining = remaining;
            this.total = total;
        }

        public boolean isEnough() {
            return enough;
        }

        public int getRemaining() {
            return remaining;
        }

        public int getTotal() {
            return total;
        }
    }
}
