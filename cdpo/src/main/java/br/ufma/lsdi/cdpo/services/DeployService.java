package br.ufma.lsdi.cdpo.services;

import br.ufma.lsdi.cdpo.models.Epn;
import org.springframework.stereotype.Service;

/*
Classe de serviço com métodos para realizar o deploy da epn na rede.
 */
@Service
public class DeployService {

    public void deploy(Epn epn) {

        epn.getRules().stream().forEach(rule -> {
            // deploy each rule

        });

    }

}
