package org.tvr.YourCalendar.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.tvr.YourCalendar.models.Action;
import org.tvr.YourCalendar.models.Person;
import org.tvr.YourCalendar.services.ActionService;
import org.tvr.YourCalendar.services.PersonService;
import org.tvr.YourCalendar.utils.ActionValidator;
import org.tvr.YourCalendar.utils.CalendarWrap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Optional;

@Controller
@RequestMapping("/days")
public class ActionController {
    private CalendarWrap calendar;
    private final PersonService personService;
    private final ActionService actionService;
    private final ActionValidator actionValidator;
    @Autowired
    public ActionController(CalendarWrap calendar, PersonService personService, ActionService actionService, ActionValidator actionValidator) {
        this.calendar = calendar;
        this.personService = personService;
        this.actionService = actionService;
        this.actionValidator = actionValidator;
    }
    @ModelAttribute("authUsername")
    public String getAuthUsername(Authentication authentication){
        Person person = personService.getAuthPerson(authentication);
        return person!=null ? person.getUsername():null;
    }
    @ModelAttribute("calendar")
    public CalendarWrap getCalendar(){
        return this.calendar;
    }

    @GetMapping("/{day}")
    public String day(@PathVariable int day, Model model, Authentication authentication){
        calendar.setDay(day);
        Person person = personService.getAuthPerson(authentication);
        model.addAttribute("actions", actionService.getActionsForThisDay(person,calendar));
        return "day";
    }
    @GetMapping("/addAction")
    public String addAction(@ModelAttribute("action") Action action){
        return "addAction";
    }
    @PostMapping("/addAction")
    public String addActionPost(@ModelAttribute ("action")  @Valid Action action, BindingResult bindingResult,
                                @RequestParam("time") String time, Authentication authentication, Model model,
                                HttpServletRequest request) {
        if (time.isEmpty()) {
            model.addAttribute("timeError","Укажите время");
            return "addAction";
        }
        String[] hhMM=time.split(":");
        int hour = Integer.parseInt(hhMM[0]);
        int minute = Integer.parseInt(hhMM[1]);
        LocalDateTime date = LocalDateTime.of(calendar.getYear(),calendar.getMonth()+1,calendar.getDay(), hour, minute);
        action.setDate(date);

        actionValidator.validate(action,bindingResult);
        if (bindingResult.hasErrors()) {
            return "addAction";
        }
        Person person = personService.getAuthPerson(authentication);
        action.setOwner(person);
        actionService.save(action,person);
        return "redirect:/days/"+calendar.getDay();
    }
    @GetMapping("/infoAction/{id}")
    public String info(@PathVariable int id,Authentication authentication,Model model){
        Person person = personService.getAuthPerson(authentication);
        Optional<Action> actionOp = person.getItems().stream().filter(action ->action.getId()==id).findFirst();
        if (actionOp.isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("repeatForModel",actionService.repeatTextForModel(actionOp.get().getRepeat()));
        model.addAttribute("past", calendar.toLocalDate().isAfter(LocalDate.now()));
        model.addAttribute("action",actionOp.get());
        return "actionInfo";
    }
    @PostMapping("/infoAction/{id}")
    public String done(@PathVariable int id,Authentication authentication){
        actionService.done(id,calendar.toLocalDate(),authentication);
        return "redirect:/days/"+calendar.getDay();
    }
    @PostMapping("/deleteAction/{id}")
    public String deleteAction(@PathVariable int id,Authentication authentication,HttpServletRequest request) {
        actionService.delete(id);
        return "redirect:/days/"+calendar.getDay();
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable int id,Model model){
        Action action=actionService.findById(id);
        //Нужен рефактор!
        calendar.getCalendar().set(Calendar.HOUR_OF_DAY,action.getDate().getHour());
        calendar.getCalendar().set(Calendar.MINUTE,action.getDate().getMinute());
        calendar.getCalendar().set(Calendar.SECOND,0);
        action.setDate(calendar.getCalendar().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        model.addAttribute("action",action);
        model.addAttribute("repeat",action.getRepeat());
        return "editAction";
    }
    @PostMapping("/edit/{id}")
    public String editPost(@ModelAttribute @Valid Action action, BindingResult bindingResult,
                           HttpServletRequest request,Model model, @PathVariable int id, Authentication authentication){
        actionValidator.validate(action,bindingResult);
        Action forChange = actionService.findById(id);
        Person person = personService.getAuthPerson(authentication);
        if (bindingResult.hasErrors()) {
            if (bindingResult.hasFieldErrors("repeat")) {
                model.addAttribute("repeat",forChange.getRepeat());
            }
            return "editAction";
        }
        if (request.getParameter("saveOne")!=null) {
            actionService.done(forChange.getId(),calendar.toLocalDate(),authentication);
            action.setOwner(personService.getAuthPerson(authentication));
            action.setId(null);
            actionService.save(action,person);
        }
        if (request.getParameter("saveAll")!=null) {
            actionService.editAllAction(action,forChange,person);

        }
        return "redirect:/";
    }
}
