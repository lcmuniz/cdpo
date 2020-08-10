package br.ufma.lsdi.tagger.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "tagger")
@Getter
@Setter
public class TaggerProperties {
    private Map<String, String> providerUrl;
}
