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
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
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
            IMResult<Void> result = ConferenceAdmin.createRoom(info.conferenceId, info.conferenceTitle, info.pin, 9, info.advance, 0, false);
            if(result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                conferenceEntityRepository.save(convertConference(info));
                favConference(info.conferenceId);
                return RestResult.ok(info.conferenceId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult destroyConference(String conferenceId) {
        String userId = getUserId();
        Optional<ConferenceEntity> conferenceEntityOptional = conferenceEntityRepository.findById(conferenceId);
        if(conferenceEntityOptional.isPresent()) {
            ConferenceEntity entity = conferenceEntityOptional.get();
            if(!userId.equals(entity.owner)) {
                return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
            }

            try {
                ConferenceAdmin.destroy(entity.id, entity.advance);
            } catch (Exception e) {
                e.printStackTrace();
            }
            conferenceEntityRepository.deleteById(conferenceId);
        } else {
            try {
                IMResult<PojoConferenceInfoList> conferenceInfoListIMResult = ConferenceAdmin.listConferences();
                if(conferenceInfoListIMResult != null && conferenceInfoListIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                    for (PojoConferenceInfo info : conferenceInfoListIMResult.getResult().conferenceInfoList) {
                        if(info.roomId.equals(conferenceId)) {
                            ConferenceAdmin.destroy(info.roomId, info.advance);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return RestResult.ok(null);
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
        return info;
    }

    private String getUserId() {
        Subject subject = SecurityUtils.getSubject();
        return (String) subject.getSession().getAttribute("userId");
    }
}
