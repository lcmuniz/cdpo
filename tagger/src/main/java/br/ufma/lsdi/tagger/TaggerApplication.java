package br.ufma.lsdi.tagger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class TaggerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaggerApplication.class, args);
    }

}
