package com.attila.bookingsystem.service;

import com.attila.bookingsystem.domain.AppUser;
import com.attila.bookingsystem.domain.AuditLog;
import com.attila.bookingsystem.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Központi belépési pont az audit_log-ba íráshoz - a hívó service-eknek nem kell
 * közvetlenül az AuditLogRepository-val/entitás-konstruálással bajlódniuk.
 * Szándékosan NINCS itt saját @Transactional: a record() mindig a HÍVÓ (üzleti
 * művelet) tranzakciójában fut, így ha az a művelet rollback-el, az audit
 * bejegyzés is vele együtt tűnik el - nem akarunk sort egy olyan eseményről,
 * ami ténylegesen NEM történt meg.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(AppUser actor, String action, String entityType, UUID entityId, Map<String, Object> details) {
        auditLogRepository.save(new AuditLog(actor, action, entityType, entityId, details));
    }

    public void record(AppUser actor, String action, String entityType, UUID entityId) {
        record(actor, action, entityType, entityId, null);
    }
}
