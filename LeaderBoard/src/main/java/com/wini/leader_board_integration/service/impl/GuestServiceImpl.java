package com.wini.leader_board_integration.service.impl;

import com.wini.leader_board_integration.data.enums.LoginPlatformType;
import com.wini.leader_board_integration.data.model.Country;
import com.wini.leader_board_integration.data.model.DefaultConfig;
import com.wini.leader_board_integration.data.model.GamePlatformLink;
import com.wini.leader_board_integration.data.model.Profile;
import com.wini.leader_board_integration.data.model.security.User;
import com.wini.leader_board_integration.service.*;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Created by kamal on 1/8/2019.
 */
@Service
public class GuestServiceImpl implements GuestService {
    private static final Logger logger = LoggerFactory.getLogger(com.wini.leader_board_integration.service.impl.GuestServiceImpl.class);

    private final ProfileService profileService;
    private final UserService userService;
    private final DefaultConfigService defaultConfigService;
    private final CountryService countryService;

    public GuestServiceImpl(ProfileService profileService, UserService userService, DefaultConfigService defaultConfigService, CountryService countryService) {
        this.profileService = profileService;
        this.userService = userService;
        this.defaultConfigService = defaultConfigService;
        this.countryService = countryService;
    }

    @Override
    public Profile createGuest(HttpServletRequest request, final GamePlatformLink gamePlatformLink, String ip) {
        DefaultConfig defaultConfig = defaultConfigService.findByGamePlatformLinkId(gamePlatformLink.getId());
        Profile profile = new Profile();
        profile.setGamePlatformLinkId(gamePlatformLink.getId());

        defaultConfig.getPrivateData().put("supportId", ObjectId.get().toString());

        profile.setPrivateData(defaultConfig.getPrivateData());
        profile.setMutableData(defaultConfig.getMutableData());

        Map<String, Object> publicData = defaultConfig.getPublicData();

        try {
            Country country = countryService.findByIp(InetAddress.getByName(countryService.getIpAddress(request)));
            if (country != null) {
                publicData.put("country", country.getCountryCode());
                publicData.put("countryImageUrl", country.getImageUrl());
            }
        } catch (UnknownHostException e) {
        }
        profile.setPublicData(publicData);
        profile = profileService.save(profile);
        User user = userService.getUser(profile, "Guest", LoginPlatformType.Guest,ip);
        publicData.replace("name", user.getUsername());

        profile.setPublicData(publicData);
        profile.setUserId(user.getUserId());
        profile = profileService.save(profile);
        logger.info("gamePlatformLinkId={} guestId={} date={}", profile.getGamePlatformLinkId(), profile.getId(), LocalDateTime.now());
        return profile;
    }


}
