package org.tvr.YourCalendar.utils;

import org.springframework.context.ApplicationListener;
import org.springframework.security.core.session.*;
import org.springframework.stereotype.Component;
import org.tvr.YourCalendar.security.PersonDetails;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Component
public class MySessionRegistry implements SessionRegistry, ApplicationListener<AbstractSessionEvent> {
    private ConcurrentMap<PersonDetails, Set<String>> persons;
    private Map<String, SessionInformation> sessionIds;

    public MySessionRegistry() {
        this.persons = new ConcurrentHashMap();
        this.sessionIds = new ConcurrentHashMap();
    }
    @Override
    public List<Object> getAllPrincipals() {
        return new ArrayList(this.persons.keySet());
    }

    @Override
    public List<SessionInformation> getAllSessions(Object personDetails, boolean includeExpiredSessions) {
        Set<String> userSessionsId = persons.get((PersonDetails) personDetails);
        if (userSessionsId!=null) {
            if (!includeExpiredSessions) {
                return userSessionsId.stream().map(sessionIds::get)
                        .filter(sessionInformation->!sessionInformation.isExpired()).collect(Collectors.toList());
            }
            else {
                return userSessionsId.stream().map(sessionIds::get).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public SessionInformation getSessionInformation(String sessionId) {
        return sessionIds.get(sessionId);
    }

    @Override
    public void refreshLastRequest(String sessionId) {
        SessionInformation info = getSessionInformation(sessionId);
        if (info != null) {
            info.refreshLastRequest();
        }
    }

    @Override
    public void registerNewSession(String sessionId, Object personDetails) {
        sessionIds.put(sessionId, new SessionInformation((PersonDetails)personDetails, sessionId, new Date()));
        if (persons.containsKey((PersonDetails) personDetails)) {
            persons.get((PersonDetails)personDetails).add(sessionId);
        }
        else {
            Set<String> sessions =  new CopyOnWriteArraySet<>();
            sessions.add(sessionId);
            persons.put((PersonDetails) personDetails,sessions);
        }
    }

    @Override
    public void removeSessionInformation(String sessionId) {
        SessionInformation info = this.getSessionInformation(sessionId);
        if (info != null) {
            sessionIds.remove(sessionId);
            persons.computeIfPresent((PersonDetails) info.getPrincipal(), (key, sessionsUsedByPrincipal) ->
                    {
                        sessionsUsedByPrincipal.remove(sessionId);
                        return sessionsUsedByPrincipal;
                    });
            if (persons.get((PersonDetails)info.getPrincipal()).isEmpty()) {
                persons.remove((PersonDetails)info.getPrincipal());
            }
        }
    }

    @Override
    public void onApplicationEvent(AbstractSessionEvent event) {
        String oldSessionId;
        if (event instanceof SessionDestroyedEvent sessionDestroyedEvent) {
            oldSessionId = sessionDestroyedEvent.getId();
            this.removeSessionInformation(oldSessionId);
        } else if (event instanceof SessionIdChangedEvent sessionIdChangedEvent) {
            oldSessionId = sessionIdChangedEvent.getOldSessionId();
            if (this.sessionIds.containsKey(oldSessionId)) {
                Object principal = ((SessionInformation)this.sessionIds.get(oldSessionId)).getPrincipal();
                this.removeSessionInformation(oldSessionId);
                this.registerNewSession(sessionIdChangedEvent.getNewSessionId(), (PersonDetails)principal);
            }
        }
    }
}
