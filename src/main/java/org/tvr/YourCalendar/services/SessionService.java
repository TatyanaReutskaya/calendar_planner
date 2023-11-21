package org.tvr.YourCalendar.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.tvr.YourCalendar.models.Person;
import org.tvr.YourCalendar.security.PersonDetails;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

@Service
public class SessionService {
    private SessionRegistry sessionRegistry;
    @Autowired
    public SessionService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }
    //нужно пользоваться методами session registry для изменений
    public void changePrincipal(PersonDetails oldPerson, PersonDetails newPerson) {
        List<SessionInformation> oldSI = sessionRegistry.getAllSessions(oldPerson,true);
        for(int i = 0;i<oldSI.size();i++) {
            String sessionId = oldSI.get(i).getSessionId();
            sessionRegistry.removeSessionInformation(sessionId);
            sessionRegistry.registerNewSession(sessionId, newPerson);
        }
    }
    public void expireAll(Object oldPersonDetails,String id) {
        sessionRegistry.getAllSessions(oldPersonDetails,false).stream()
                .filter(si->si.getSessionId()!=id).forEach(SessionInformation::expireNow);
    }

    public void registerNewSession(Person person, HttpServletRequest request) {
        sessionRegistry.registerNewSession(request.getSession(false).getId(),new PersonDetails(person));
    }
    public void forChangeAllSession(PersonDetails pd){
        sessionRegistry.getAllSessions(pd,false).stream()
                .peek(si->pd.changeNeedSession.add(si.getSessionId()))
                .map(SessionInformation::getSessionId)
                .forEach(id->sessionRegistry.registerNewSession(id,pd));
    }
    public List<SessionInformation> test1(PersonDetails pd) {
        return sessionRegistry.getAllSessions(pd,false);
    }
    public PersonDetails checkChange(String sessionId) {
        return (PersonDetails) sessionRegistry.getSessionInformation(sessionId).getPrincipal();
    }
}