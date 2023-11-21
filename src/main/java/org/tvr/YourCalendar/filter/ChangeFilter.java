package org.tvr.YourCalendar.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.tvr.YourCalendar.security.PersonDetails;
import org.tvr.YourCalendar.services.PersonService;
import org.tvr.YourCalendar.services.SessionService;

import java.io.IOException;
@WebFilter(urlPatterns = {"/","/settings","/days/*"})
public class ChangeFilter extends OncePerRequestFilter {
    private final PersonService personService;
    private final SessionService sessionService;
    @Autowired
    public ChangeFilter(PersonService personService, SessionService sessionService) {
        this.personService = personService;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession();
        SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (securityContext!=null){
            Authentication authentication = securityContext.getAuthentication();
            PersonDetails personDetails = sessionService.checkChange(request.getSession(false).getId());
            if(!personDetails.changeNeedSession.isEmpty()&&personDetails.changeNeedSession.contains(request.getSession(false).getId())) {
                System.out.println("Изменения проводятся");
                personDetails.changeNeedSession.removeIf(str->str.equals(request.getSession(false).getId()));
                Authentication newAuthentication = new UsernamePasswordAuthenticationToken(personDetails, authentication.getCredentials(), authentication.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(newAuthentication);
                request.getSession(false).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
            }
        }
        filterChain.doFilter(request,response);
    }
}
