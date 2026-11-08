package sn.samatur.samatur.alerte.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlerteScheduler {

    @Autowired
    private AlerteService alerteService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void verifierRetardsQuotidien() {
        System.out.println("Execution quotidienne de la verification des retards de dossiers...");
        alerteService.verifierRetardsDossiers();
        System.out.println("Verification des retards terminee.");
    }
}
