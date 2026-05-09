package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.UserConfigDTO;
import com.aick.mmp.central.repository.UserConfigRepository;
import com.aick.mmp.central.service.UserConfigService;
import com.aick.mmp.shared.model.UserConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserConfigServiceImpl implements UserConfigService {
    
    private final UserConfigRepository userConfigRepository;
    
    @Override
    @Transactional
    public UserConfig saveConfig(Long userId, String configKey, String configValue) {
        log.debug("Saving config for user {} with key {}", userId, configKey);
        
        Optional<UserConfig> existingConfig = userConfigRepository.findByUserIdAndConfigKey(userId, configKey);
        
        if (existingConfig.isPresent()) {
            UserConfig config = existingConfig.get();
            config.setConfigValue(configValue);
            return userConfigRepository.save(config);
        } else {
            UserConfig config = UserConfig.builder()
                    .userId(userId)
                    .configKey(configKey)
                    .configValue(configValue)
                    .build();
            return userConfigRepository.save(config);
        }
    }
    
    @Override
    @Transactional
    public List<UserConfig> saveConfigs(Long userId, List<UserConfigDTO> configs) {
        log.debug("Saving {} configs for user {}", configs.size(), userId);
        return configs.stream()
                .map(dto -> saveConfig(userId, dto.getConfigKey(), dto.getConfigValue()))
                .toList();
    }
    
    @Override
    public Optional<UserConfig> getConfig(Long userId, String configKey) {
        return userConfigRepository.findByUserIdAndConfigKey(userId, configKey);
    }
    
    @Override
    public List<UserConfig> getUserConfigs(Long userId) {
        return userConfigRepository.findByUserId(userId);
    }
    
    @Override
    @Transactional
    public void deleteConfig(Long userId, String configKey) {
        log.debug("Deleting config for user {} with key {}", userId, configKey);
        userConfigRepository.deleteByUserIdAndConfigKey(userId, configKey);
    }
    
    @Override
    @Transactional
    public void deleteUserConfigs(Long userId) {
        log.debug("Deleting all configs for user {}", userId);
        userConfigRepository.deleteByUserId(userId);
    }
}
