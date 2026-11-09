package sn.samatur.samatur.alerte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Chaque nuit, enregistre une alerte pour chaque dossier devenu en retard depuis la veille (RG-11). */
@Component
public class AlerteScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AlerteScheduler.class);

    private final AlerteService alerteService;

    public AlerteScheduler(AlerteService alerteService) {
        this.alerteService = alerteService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Africa/Dakar")
    public void verifierRetardsQuotidien() {
        int creees = alerteService.enregistrerNouvellesAlertes();
        // Seulement un nombre : jamais de nom d'élève dans les logs
        logger.info("Vérification quotidienne des retards : {} nouvelle(s) alerte(s).", creees);
    }
}
