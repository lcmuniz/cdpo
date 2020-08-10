package br.ufma.lsdi.tagger.services;

import br.ufma.lsdi.tagger.config.TaggerProperties;
import br.ufma.lsdi.tagger.entities.ObjectType;
import br.ufma.lsdi.tagger.repos.ObjectTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SeedObjectTypeService {
    @Autowired
    ObjectTypeRepository objectTypeRepository;
    @Autowired
    TaggerProperties taggerProperties;
    Logger logger = LoggerFactory.getLogger(SeedObjectTypeService.class);

    public void seedObjectType() {
        if(taggerProperties.getProviderUrl()==null) return;
        // Para cada propriedade com nome tagger.provider-url.* executa a lógica para criar ou atualizar ObjectType
        taggerProperties.getProviderUrl()
                .entrySet().stream()
                    .forEach(e -> seedObjectType(e.getKey(), e.getValue()));
    }

    private void seedObjectType(String type, String providerUrl) {

        // Retira hífens '-' e torna maiusculo as iniciais
        String typeNormalized = normalize(type);
        // Pega o objeto type do banco ou cria um novo caso não exista
        ObjectType objectType =
                objectTypeRepository.findByType(typeNormalized)
                        .orElseGet(() -> new ObjectType(UUID.randomUUID().toString(),typeNormalized, providerUrl));

        // Cria um alerta caso a providerUrl tenha sido alterada
        if(objectType.getProviderUrl().equals(providerUrl))
            logger.info("Provider URL by Property: "+ typeNormalized + " -> " + providerUrl);
        else
            logger.warn("Provider URL from "+ typeNormalized + " was changed for: " + providerUrl);

        // Seta e salva o novo valor
        objectType.setProviderUrl(providerUrl);
        objectTypeRepository.save(objectType);
    }

    // Substitui elimina os '-' e torna maiuscula apenas as primeiras letras
    private String normalize(String str){
        StringBuilder builder = new StringBuilder("");
        char [] chars = str.toCharArray();

        boolean toUpper = true;

        for(int i =0;i<chars.length;i++){
            if(chars[i]=='-'){
                toUpper =true;
                continue;
            }
            if (toUpper)
                builder.append(Character.toUpperCase(chars[i]));
            else builder.append(Character.toLowerCase(chars[i]));

            toUpper = false;
        }

        return builder.toString();
    }
}
