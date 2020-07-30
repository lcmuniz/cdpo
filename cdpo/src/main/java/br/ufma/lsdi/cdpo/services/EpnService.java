package br.ufma.lsdi.cdpo.services;

import br.ufma.lsdi.cdpo.entities.*;
import br.ufma.lsdi.cdpo.repos.EpnRepository;
import br.ufma.lsdi.cdpo.repos.EventTypeAttributeRepository;
import br.ufma.lsdi.cdpo.repos.EventTypeRepository;
import br.ufma.lsdi.cdpo.repos.RuleRepository;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/*
Classe de serviço com métodos para tratar epns.
 */
@Service
public class EpnService {

    private final EpnRepository epnRepository;
    private final RuleRepository ruleRepository;
    private final EventTypeRepository eventTypeRepository;
    private final EventTypeAttributeRepository eventTypeAttributeRepository;

    public EpnService(EpnRepository epnRepository, RuleRepository ruleRepository, EventTypeRepository eventTypeRepository, EventTypeAttributeRepository eventTypeAttributeRepository) {
        this.epnRepository = epnRepository;
        this.ruleRepository = ruleRepository;
        this.eventTypeRepository = eventTypeRepository;
        this.eventTypeAttributeRepository = eventTypeAttributeRepository;
    }

    public Epn save(Epn epn) {
        val _epn = fillUuids(epn);
        return epnRepository.save(_epn);
    }

    private Epn fillUuids(Epn epn) {
        val _epn = epn;
        if (_epn.getUuid() == null) _epn.setUuid(UUID.randomUUID().toString());
        if (_epn.getRules() == null) _epn.setRules(new ArrayList<>());
        _epn.getRules().forEach(r -> {
            if (r.getUuid() == null) r.setUuid(UUID.randomUUID().toString());
            r.setEpn(_epn);
            if (r.getEventTypes() == null) r.setEventTypes(new ArrayList<>());
            r.getEventTypes().forEach(et -> {
                if (et.getUuid() == null) et.setUuid(UUID.randomUUID().toString());
                et.setRule(r);
                if (et.getAttributes() == null) et.setAttributes(new ArrayList<>());
                et.getAttributes().forEach(att -> {
                    if (att.getUuid() == null) att.setUuid(UUID.randomUUID().toString());
                    att.setEventType(et);
                });
            });
        });
        return _epn;
    }

}
