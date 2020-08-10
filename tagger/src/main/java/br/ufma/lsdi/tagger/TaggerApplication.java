package br.ufma.lsdi.tagger;

import br.ufma.lsdi.tagger.services.SeedObjectTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class TaggerApplication {
    @Autowired
    SeedObjectTypeService seedObjectTypeService;

    public static void main(String[] args) {
        SpringApplication.run(TaggerApplication.class, args);
    }

    @EventListener
    public void seed(ContextRefreshedEvent event) {
        seedObjectTypeService.seedObjectType();
    }
}
