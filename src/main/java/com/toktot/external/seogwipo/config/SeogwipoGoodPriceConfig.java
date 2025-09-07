package com.toktot.external.seogwipo.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeogwipoGoodPriceConfig {

    @Bean
    public XmlMapper seogwipoXmlMapper() {
        return new XmlMapper();
    }
}
