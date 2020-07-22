package br.ufma.lsdi.cdpo;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Deploy {
    @Id
    private String uuid;
    private String hostUuuid;
}
