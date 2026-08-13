package com.kiborisaway.tasktimetracker.service;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class SessionInvalidationService {

  private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

  public SessionInvalidationService(
      FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  public void invalidateAll(int userId) {
    sessionRepository.findByPrincipalName(Integer.toString(userId))
        .keySet()
        .forEach(sessionRepository::deleteById);
  }
}
