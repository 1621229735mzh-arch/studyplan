package com.kaoyan.study.review.service;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** No changes from GET requests. Enabled books are reconciled after startup/cutoff/downtime. */
@Component
@EnableScheduling
@ConditionalOnProperty(name="app.review.automatic-reconciliation-enabled",havingValue="true",matchIfMissing=true)
public class AutoReviewReconciler {
    private final AutoReviewService service;
    public AutoReviewReconciler(AutoReviewService service) { this.service=service; }
    @Scheduled(initialDelay=60000,fixedDelay=60000)
    public void run() {
        for (var book:service.books()) if (book.enabled()) {
            try { service.reconcile(book.streamId()); }
            catch (RuntimeException ex) { LoggerFactory.getLogger(getClass()).error("Automatic review reconciliation failed for book {}",book.streamId(),ex); }
        }
    }
}
