package com.starise.ipn.service;



import com.starise.ipn.Util.Logger;
import com.starise.ipn.config.SchemaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;


@Component("CoreServices")
public class MediumsControllerService implements ApplicationListener<ContextRefreshedEvent> {
    public static SchemaConfig schemaConfig;
    public static Logger logHandler;

    @Autowired
    public void setSchemaConfig(SchemaConfig schemaConfig) {
        MediumsControllerService.schemaConfig = schemaConfig;
    }

    @Autowired
    public void setLogger(Logger logHandler) {
        MediumsControllerService.logHandler = logHandler;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

    }
}
