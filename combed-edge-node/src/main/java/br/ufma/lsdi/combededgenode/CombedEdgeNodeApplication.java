package br.ufma.lsdi.combededgenode;

import br.ufma.lsdi.combededgenode.models.Power;
import br.ufma.lsdi.combededgenode.services.CepService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class CombedEdgeNodeApplication {
    @Autowired
    CepService cepService;

    @Value("${combed.power.file}")
    String filePath;

    public static void main(String[] args) {
        SpringApplication.run(CombedEdgeNodeApplication.class, args);
    }

    @EventListener
    public void startFileRead(ContextRefreshedEvent event){
        val map = new HashMap<>();
        map.put("timestamp", Long.class);
        map.put("power", Double.class);
        cepService.addEventType("Power", map);

        val stm = cepService.addRule("select * from Power", "Power");
        stm.addListener((eventBeans, eventBeans1) -> {
            val power = (Map) eventBeans[0].getUnderlying();
            System.out.println(">>> Power - " + power);
        });

        new Thread(() -> {
            try {
                this.job();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void job() throws InterruptedException{
        try(Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                Thread.sleep(5*1000);
                Power p = readCsvLine(scanner.nextLine());

                cepService.send(p.toMap(), "Power");
            }
        } catch (FileNotFoundException  e) {
            e.printStackTrace();
        }

    }

    private Power readCsvLine(String line){
        Power res = new Power();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");
            String timestampString = rowScanner.next();
            Double timestampDouble = Double.valueOf(timestampString);
            res.setTimestamp(timestampDouble.longValue());

            res.setPower(Double.valueOf(rowScanner.next()));
        }
        return res;
    }

}
