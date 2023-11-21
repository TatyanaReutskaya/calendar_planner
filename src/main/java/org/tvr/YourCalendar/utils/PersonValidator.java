package org.tvr.YourCalendar.utils;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.tvr.YourCalendar.models.Person;
import org.tvr.YourCalendar.services.PersonService;

@Component
public class PersonValidator implements Validator {
    private final PersonService personService;
    @Autowired
    public PersonValidator(PersonService personService) {
        this.personService = personService;
    }
    @Override
    public boolean supports(Class<?> clazz) {
        return PersonValidator.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Person person = (Person) target;
        if (personService.findByEmail(person.getEmail()).isPresent()) {
            errors.rejectValue("email","","A User whith this email already exists.");
        }
        if(person.getPassword().isEmpty()) {
            errors.rejectValue("password","","Password shouldn`t be empty.");
        }
    }
}