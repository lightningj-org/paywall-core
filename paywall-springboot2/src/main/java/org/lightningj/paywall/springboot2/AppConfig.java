package org.lightningj.paywall.springboot2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// Here all bean definitions

// TODO create Implementation of everything for local

@ComponentScan("org.lightningj.paywall.spring")
@Configuration
public class AppConfig {

    @Bean
    public PocResult getGreeting(){
        return new PocResult(1,"asdf");
    }


}
