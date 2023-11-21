package org.tvr.YourCalendar.utils;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.tvr.YourCalendar.models.Action;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ActionValidator implements Validator{
    @Override
    public boolean supports(Class<?> clazz) {
        return ActionValidator.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Action action = (Action) target;
        LocalDateTime dateNow = LocalDateTime.now();
        if(action.getDate().isBefore(dateNow)&&action.getRepeat()==null){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            errors.rejectValue("repeat","",String.format("Не получится добавить событие, так как %s уже прошло, а повтор события не установлен",
                    action.getDate().format(formatter)));
        }
    }
}
