package org.tvr.YourCalendar.services;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.tvr.YourCalendar.exception.UrlFromMailException;
import org.tvr.YourCalendar.models.Person;
import org.tvr.YourCalendar.repositories.PeopleRepository;
import org.tvr.YourCalendar.security.PersonDetails;

import java.util.Optional;
@Service
@Transactional(readOnly = true)
public class PersonService {
    private final PeopleRepository peopleRepository;
    private final EncodeDecodeServise encodeDecodeServise;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private SessionService sessionService;
    @Autowired
    public PersonService(PeopleRepository peopleRepository,
                         EncodeDecodeServise encodeDecodeServise,
                         EmailService emailService,
                         PasswordEncoder passwordEncoder) {
        this.peopleRepository = peopleRepository;
        this.encodeDecodeServise = encodeDecodeServise;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional (isolation = Isolation.SERIALIZABLE)
    public void save(Person person) {
        String encodePass = encodeDecodeServise.encode(person.getPassword());
        person.setPassword(encodePass);
        person.setEnable(false);
        emailService.sendMailForFinishRegistration(person);
        peopleRepository.save(person);
    }
    @Transactional
    public void updateWhitoutEmail(Person person, Authentication authentication,HttpServletRequest request) {
        Person forChange = peopleRepository.findByEmail(person.getEmail()).orElse(null);
        if (forChange!=null) {
            forChange.setUsername(person.getUsername());
            forChange.setDateOfBirth(person.getDateOfBirth());
            peopleRepository.save(forChange);
            sessionService.forChangeAllSession(new PersonDetails(forChange));
        }
    }
    @Transactional (isolation = Isolation.SERIALIZABLE)
    public void updateWhithEmail(Person person,Authentication authentication) {
        Person forChange = peopleRepository.findById(person.getId()).orElse(null);
        if (forChange!=null) {
            forChange.setUsername(person.getUsername());
            forChange.setDateOfBirth(person.getDateOfBirth());
            peopleRepository.save(forChange);
            emailService.sendMailForUpdateEmail(forChange.getEmail(),person.getEmail());
            sessionService.forChangeAllSession(new PersonDetails(forChange));
        }
    }
    @Transactional
    public void changeEmail(String encodedString,Authentication authentication,HttpServletRequest request){
        String[] emails = encodeDecodeServise.decode(encodedString).split("\\|");
        Person person = findByEmail(emails[0]).orElse(null);
        if (person!=null) {
            person.setEmail(emails[1]);
            sessionService.forChangeAllSession(new PersonDetails(person));
            peopleRepository.save(person);
        }
        else {
            throw new UrlFromMailException("Извините, ссылка не дествительна :(");
        }
    }
    @Transactional
    public void finishRegistration(String encodedString, HttpServletRequest request) {
        String email = encodeDecodeServise.decode(encodedString);
        Person person = findByEmail(email).orElse(null);
        if (person!=null) {
            //При втором переходе по этой же ссылке будет IllegalArgumentException в decoder.decode(text)
            //т.к. в базе уже храниться хеш пароля, а не закодированный encodeDecodeServise пароль как при первом переходе по ссылке
            String password = encodeDecodeServise.decode(person.getPassword());
            person.setPassword(passwordEncoder.encode(password));
            person.setEnable(true);
            peopleRepository.save(person);
            try {
                request.login(person.getEmail(), password);
            } catch (ServletException e) {
                e.printStackTrace();
            }
            sessionService.registerNewSession(person,request);
        }
        else {
            throw new UrlFromMailException("Извините, ссылка более не дествительна :(");
        }
    }
    @Transactional
    public boolean changePassword(String oldPassword,String newPassword, HttpServletRequest request,Authentication authentication){
        Person person = getAuthPerson(authentication);
        if (!passwordEncoder.matches(oldPassword,person.getPassword())) {
            return false;
        }
        person.setPassword(passwordEncoder.encode(newPassword));
        peopleRepository.save(person);
        sessionService.expireAll(new PersonDetails(person),request.getSession(false).getId());
        sessionService.forChangeAllSession(new PersonDetails(person));
        return true;
    }
    public void restorePassword(Person person,HttpServletRequest request){
        emailService.sendMailForRestorePassword(person);
    }
    @Transactional
    public void restorePasswordFromMail(HttpServletRequest request,String newPassword) {
        String[] encodeFromMail = encodeDecodeServise.decode((String)request.getSession().getAttribute("encode")).split("\\|");
        Optional<Person> personOptional = findByEmail(encodeFromMail[0]);
        if (personOptional.isPresent()) {
            if(!encodeFromMail[1].equals(personOptional.get().getPassword())) {
                throw new UrlFromMailException("Извините, ссылка не действительна");
            }
        }
        else {
            throw new UrlFromMailException("Извините, ссылка не действительна");
        }
        Person person = personOptional.get();
        sessionService.expireAll(new PersonDetails(person),request.getSession(false).getId());
        person.setPassword(passwordEncoder.encode(newPassword));
        peopleRepository.save(person);
        try {
            request.login(person.getEmail(), newPassword);
        } catch (ServletException e) {
            e.printStackTrace();
        }
        sessionService.registerNewSession(person,request);
    }
    public Optional<Person> findByEmail(String email) {
        return peopleRepository.findByEmail(email);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanNotEnabledPerson() {
        peopleRepository.cleanNotEnabled();
    }

    public Person getAuthPerson(Authentication authentication) {
        if (authentication!=null) {
            PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
            return personDetails.getPerson();
        }
        else {
            return null;
        }
    }

    public Optional<Person> findById(int id) {
        return peopleRepository.findById(id);
    }
}
