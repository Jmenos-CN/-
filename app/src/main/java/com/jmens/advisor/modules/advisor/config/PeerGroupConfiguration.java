package com.jmens.advisor.modules.advisor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PeerGroupProperties.class)
public class PeerGroupConfiguration {
}
