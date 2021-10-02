package cn.wildfirechat.app.ptt;


import cn.wildfirechat.app.IMConfig;
import cn.wildfirechat.app.RestResult;
import cn.wildfirechat.app.pojo.PttChannelInfo;
import cn.wildfirechat.app.tools.NumericIdGenerator;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.sdk.AdminConfig;
import cn.wildfirechat.sdk.ConferenceAdmin;
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
public class PttServiceImpl implements PttService {
    private static final Logger LOG = LoggerFactory.getLogger(PttServiceImpl.class);


    @Autowired
    private IMConfig mIMConfig;
//
//    @Autowired
//    private ChannelEntityRepository ChannelEntityRepository;
//
//    @Autowired
//    private UserPrivateChannelIdRepository userPrivateChannelIdRepository;
//
//    @Autowired
//    private UserChannelRepository userChannelRepository;

    @PostConstruct
    private void init() {
        AdminConfig.initAdmin(mIMConfig.admin_url, mIMConfig.admin_secret);
    }

    @Override
    public RestResult getUserChannelId(String userId) {
        String ChannelId = getPrivateChannelId(userId);
        return RestResult.ok(ChannelId);
    }

    @Override
    public RestResult getMyChannelId() {
        Subject subject = SecurityUtils.getSubject();
        String userId = (String) subject.getSession().getAttribute("userId");
        return getUserChannelId(userId);
    }

    private String getPrivateChannelId(String userId) {
//        Optional<UserPrivateChannelId> privateChannelIdOptional = userPrivateChannelIdRepository.findById(userId);
//        if(privateChannelIdOptional.isPresent()) {
//            return privateChannelIdOptional.get().getChannelId();
//        }
//        String ChannelId = NumericIdGenerator.getId(null, Arrays.asList(0), 8);
//        userPrivateChannelIdRepository.save(new UserPrivateChannelId(userId, ChannelId));
//
//        return ChannelId;
        return userId;
    }

    @Override
    public RestResult getChannelInfo(String ChannelId, String password) {
//        Optional<ChannelEntity> ChannelEntityOptional = ChannelEntityRepository.findById(ChannelId);
//        if(ChannelEntityOptional.isPresent()) {
//            Subject subject = SecurityUtils.getSubject();
//            String userId = (String) subject.getSession().getAttribute("userId");
//            ChannelEntity entity = ChannelEntityOptional.get();
//            if(StringUtils.isEmpty(entity.password) || entity.password.equals(password) || userId.equals(entity.owner)) {
//                return RestResult.ok(convertChannel(entity));
//            }
//        }

        return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
    }

    @Override
    public RestResult putChannelInfo(PttChannelInfo info) {
//        Optional<ChannelEntity> ChannelEntityOptional = ChannelEntityRepository.findById(info.ChannelId);
//        if(ChannelEntityOptional.isPresent()) {
//            Subject subject = SecurityUtils.getSubject();
//            String userId = (String) subject.getSession().getAttribute("userId");
//            ChannelEntity entity = ChannelEntityOptional.get();
//            if(userId.equals(entity.owner)) {
//                ChannelEntityRepository.save(convertChannel(info));
//            } else {
//                return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
//            }
//        } else {
//            ChannelEntityRepository.save(convertChannel(info));
//        }

        return RestResult.ok(null);
    }

    @Override
    public RestResult createChannel(PttChannelInfo info) {
        String userId = getUserId();
        if(!StringUtils.isEmpty(info.owner)) {
            if(!info.owner.equals(userId)) {
                return RestResult.error(RestResult.RestCode.ERROR_INVALID_PARAMETER);
            }
        } else {
            info.owner = userId;
        }


        if(StringUtils.isEmpty(info.channelId)) {
            String channelId = NumericIdGenerator.getId(null, Arrays.asList(0), 14);
//            do {
//                ChannelId = NumericIdGenerator.getId(null, Arrays.asList(0), 10);
//                if(!ChannelEntityRepository.findById(ChannelId).isPresent()) {
//                    break;
//                }
//            } while (true);
            info.channelId = channelId;
        } else {
//            Optional<ChannelEntity> ChannelEntityOptional = ChannelEntityRepository.findById(info.ChannelId);
//            if(ChannelEntityOptional.isPresent()) {
//                ChannelEntity entity = ChannelEntityOptional.get();
//                if(!userId.equals(entity.owner)) {
//                    return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
//                }
//            }
        }

        try {
            IMResult<Void> result = ConferenceAdmin.createRoom(info.channelId, info.channelTitle, info.pin, 100, false, 0, false);
            if(result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
//                ChannelEntityRepository.save(convertChannel(info));
//                favChannel(info.ChannelId);
                return RestResult.ok(info.channelId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResult.error(RestResult.RestCode.ERROR_SERVER_ERROR);
    }

    @Override
    public RestResult destroyChannel(String channelId) {
        String userId = getUserId();
//        Optional<ChannelEntity> ChannelEntityOptional = ChannelEntityRepository.findById(ChannelId);
//        if(ChannelEntityOptional.isPresent()) {
//            ChannelEntity entity = ChannelEntityOptional.get();
//            if(!userId.equals(entity.owner)) {
//                return RestResult.error(RestResult.RestCode.ERROR_NO_RIGHT);
//            }

            try {
                ConferenceAdmin.destroy(channelId, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
//            ChannelEntityRepository.deleteById(ChannelId);
//        } else {
//            try {
//                IMResult<PojoChannelInfoList> ChannelInfoListIMResult = ChannelAdmin.listChannels();
//                if(ChannelInfoListIMResult != null && ChannelInfoListIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
//                    for (PojoChannelInfo info : ChannelInfoListIMResult.getResult().ChannelInfoList) {
//                        if(info.roomId.equals(ChannelId)) {
//                            ChannelAdmin.destroy(info.roomId, info.advance);
//                            break;
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        return RestResult.ok(null);
    }

    @Override
    public RestResult favChannel(String ChannelId) {
//        String userId = getUserId();
//        UserChannel userChannel = new UserChannel(userId, ChannelId);
//        userChannelRepository.save(userChannel);
        return RestResult.ok(null);
    }

    @Override
    public RestResult unfavChannel(String ChannelId) {
//        userChannelRepository.deleteByUserIdAndChannelId(getUserId(), ChannelId);
        return RestResult.ok(null);
    }

    @Override
    public RestResult getFavChannels() {
//        List<ChannelDTO> ucs = userChannelRepository.findByUserId(getUserId(), System.currentTimeMillis()/1000);
//        List<ChannelInfo> infos = new ArrayList<>();
//        for (ChannelDTO dto:ucs) {
//            ChannelInfo info = new ChannelInfo();
//            info.ChannelId = dto.getId();
//            info.ChannelTitle = dto.getChannel_title();
//            info.password = dto.getPassword();
//            info.pin = dto.getPin();
//            info.owner = dto.getOwner();
//            info.startTime = dto.getStart_time();
//            info.endTime = dto.getEnd_time();
//            info.audience = dto.isAudience();
//            info.advance = dto.isAdvance();
//            info.allowSwitchMode = dto.isAllow_switch_mode();
//            info.noJoinBeforeStart = dto.isNo_join_before_start();
//            infos.add(info);
//        }
//        return RestResult.ok(infos);
        return RestResult.ok(null);
    }

    @Override
    public RestResult isFavChannel(String ChannelId) {
//        Optional<UserChannel> userChannel = userChannelRepository.findByUserIdAndChannelId(getUserId(), ChannelId);
//        if(userChannel.isPresent())
//            return RestResult.ok(null);

        return RestResult.error(RestResult.RestCode.ERROR_NOT_EXIST);
    }
//
//    private ChannelEntity convertChannel(ChannelInfo info) {
//        ChannelEntity entity = new ChannelEntity();
//        entity.id = info.ChannelId;
//        entity.ChannelTitle = info.ChannelTitle;
//        entity.password = info.password;
//        entity.pin = info.pin;
//        entity.owner = info.owner;
//        entity.startTime = info.startTime;
//        entity.endTime = info.endTime;
//        entity.audience = info.audience;
//        entity.advance = info.advance;
//        entity.allowSwitchMode = info.allowSwitchMode;
//        entity.noJoinBeforeStart = info.noJoinBeforeStart;
//        return entity;
//    }
//
//    private ChannelInfo convertChannel(ChannelEntity entity) {
//        ChannelInfo info = new ChannelInfo();
//        info.ChannelId = entity.id;
//        info.ChannelTitle = entity.ChannelTitle;
//        info.password = entity.password;
//        info.pin = entity.pin;
//        info.owner = entity.owner;
//        info.startTime = entity.startTime;
//        info.endTime = entity.endTime;
//        info.audience = entity.audience;
//        info.advance = entity.advance;
//        info.allowSwitchMode = entity.allowSwitchMode;
//        info.noJoinBeforeStart = entity.noJoinBeforeStart;
//        return info;
//    }

    private String getUserId() {
        Subject subject = SecurityUtils.getSubject();
        return (String) subject.getSession().getAttribute("userId");
    }
}
