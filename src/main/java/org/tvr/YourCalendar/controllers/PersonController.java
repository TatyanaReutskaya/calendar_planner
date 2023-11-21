package org.tvr.YourCalendar.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tvr.YourCalendar.exception.UrlFromMailException;
import org.tvr.YourCalendar.models.Person;
import org.tvr.YourCalendar.services.ActionService;
import org.tvr.YourCalendar.services.PersonService;
import org.tvr.YourCalendar.utils.CalendarWrap;
import org.tvr.YourCalendar.utils.PersonValidator;

import java.util.Calendar;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class PersonController {
    private final PersonValidator personValidator;
    private final PersonService personService;
    private CalendarWrap calendar;
    private final ActionService actionService;
    @Autowired
    public PersonController(PersonValidator personValidator,
                            PersonService personService, ActionService actionService) {
        this.personValidator = personValidator;
        this.personService = personService;
        this.actionService = actionService;
    }
    @Autowired
    public void setCalendar(CalendarWrap calendar) {
        this.calendar = calendar;
    }
    @ModelAttribute("authUsername")
    public String getAuthUsername(Authentication authentication){
        Person person = personService.getAuthPerson(authentication);
        return person!=null ? person.getUsername():null;
    }
    @GetMapping
    public String index(Model model, Authentication authentication){
        calendar.setLastDayMonth();
        Person person = personService.getAuthPerson(authentication);
        model.addAttribute("actionInMonth",actionService.getActionDayForMonth(person,calendar));
        model.addAttribute("calendar",calendar);
        return "index";
    }
    @GetMapping("/main")
    public String mainIndex(){
        calendar.setCalendar(Calendar.getInstance());
        return "redirect:/";
    }
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("headerPNG","headerImg/summer_header.png");
        return "login";
    }
    @GetMapping("/registration")
    public String registration(@ModelAttribute("personForm") Person person) {
        return "registration";
    }
    @PostMapping("/registration")
    public String registrationPost(@ModelAttribute @Valid Person person, BindingResult bindingResult,Model model) {
        personValidator.validate(person,bindingResult);
        if (bindingResult.hasErrors()) {
            return "registration";
        }
        personService.save(person);
        String message = "Для завершения регистрации перейдите по ссылке отправленной на почту "+person.getEmail();
        model.addAttribute("message",message);
        return "message";
    }
    @GetMapping("/registration/{encodedString}")
    public String registrationFinal(@PathVariable String encodedString, HttpServletRequest request) {
        personService.finishRegistration(encodedString,request);
        return "redirect:/";
    }
    @GetMapping("/settings")
    public String settings(Authentication authentication, Model model) {
        model.addAttribute("person",personService.getAuthPerson(authentication));
        return "settings";
    }
    @GetMapping("/edit")
    public String editGet(Authentication authentication, Model model){
        model.addAttribute("person",personService.getAuthPerson(authentication));
        return "editPerson";
    }
    @PostMapping("/edit")
    public String editPost(@ModelAttribute @Valid Person person, BindingResult bindingResult,
                           Authentication authentication, HttpServletRequest request,
                           RedirectAttributes redirectAttributes){
        Person forCange = personService.getAuthPerson(authentication);
        person.setPassword(forCange.getPassword());
        if (bindingResult.hasErrors()) {
            return "editPerson";
        }
        if (person.getEmail().equals(forCange.getEmail())) {
            personService.updateWhitoutEmail(person, authentication,request);
        }
        else {
            person.setId(forCange.getId());
            personService.updateWhithEmail(person,authentication);
            String message = String.format("Для изменения email перейдите по ссылке отправленной на почту %s",person.getEmail());
            redirectAttributes.addFlashAttribute("changeEmail",message);
        }
        return "redirect:/settings";
    }
    @GetMapping("/changeEmail/{encodedString}")
    public String changeEmail(@PathVariable String encodedString,Authentication authentication, HttpServletRequest request){
        personService.changeEmail(encodedString,authentication,request);
        return "redirect:/";
    }
    @GetMapping("/editPassword")
    public String editPassword(Model model,Authentication authentication,HttpServletRequest request){
        if ((authentication==null)&&request.getSession().getAttribute("encode")==null){
            return "redirect:/login";
        }
        model.addAttribute("person",personService.getAuthPerson(authentication));
        return "editPassword";
    }
    @PostMapping("/editPassword")
    public String editPasswordPost(@RequestParam(name ="oldPassword",required = false)  String oldPassword,
                                   @RequestParam("newPassword")  String newPassword,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request, Authentication authentication){
        if(authentication!=null){
            if ((oldPassword.trim().isEmpty())||(newPassword.trim().isEmpty())) {
                if (oldPassword.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("oldPasswordError","Поле не должно быть пустым");
                }
                if (newPassword.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("newPasswordEmpty","Поле не должно быть пустым");
                }
                return "redirect:/editPassword";
            }
            if(!personService.changePassword(oldPassword, newPassword, request,authentication)) {
                redirectAttributes.addFlashAttribute("oldPasswordError","Неверный пароль");
                return "redirect:/editPassword";
            }
        }
        else {
            if (newPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("newPasswordEmpty","Поле не должно быть пустым");
                return "redirect:/editPassword";
            }
            personService.restorePasswordFromMail(request,newPassword);
            request.getSession().removeAttribute("encode");
        }
        return "redirect:/";
    }
    @GetMapping("/restorePassword")
    public String restorePassword(){
        return "restorePassword";
    }
    @PostMapping("/restorePassword")
    public String restorePasswordPost(@RequestParam("email") String email, Model model,
                                      HttpServletRequest request, RedirectAttributes redirectAttributes){
        if (email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("emailError","Поле не должно быть пустым");
            return "redirect:/restorePassword";
        }
        Optional<Person> personOptional = personService.findByEmail(email);
        if (personOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("emailError","Пользователя с таким email не существует");
            return "redirect:/restorePassword";
        }
        if (request.getParameter("restorePasswordEmail") != null) {
            personService.restorePassword(personOptional.get(),request);
            String message = "Для воостановления пароля перейдите по ссылке отправленной на почту "+personOptional.get().getEmail();
            model.addAttribute("message",message);
            return "message";
        }
        if (request.getParameter("restorePasswordTelegram") != null) {
            if (personOptional.get().getChatId()==null) {
                redirectAttributes.addFlashAttribute("emailError","Аккаунт пользователя "+email+" не привязан к Telegram");
                return "redirect:/restorePassword";
            }
            System.out.println("restorePasswordTelegram button clicked");
        }
        return "restorePassword";
    }
    @GetMapping("/restorePassword/{encodedString}")
    public String restorePasswordFromMail(@PathVariable String encodedString, HttpServletRequest request){
        request.getSession().setAttribute("encode",encodedString);
        return "redirect:/editPassword";
    }

    @GetMapping("/monthUp")
    public String monthUp() {
        calendar.monthUp();
        return "redirect:/";
    }
    @GetMapping("/monthDown")
    public String monthDown() {
        calendar.monthDown();
        return "redirect:/";
    }
    @GetMapping("/yearUp")
    public String yearUp() {
        calendar.yearUp();
        return "redirect:/";
    }
    @GetMapping("/yearDown")
    public String yearDown() {
        calendar.yearDown();
        return "redirect:/";
    }
    //При повторном переходе по ссылке для завершения регистрации с почты
    //При при переходе по ссылке по истечении времени дествия
    @ExceptionHandler(UrlFromMailException.class)
    public String decodeException(UrlFromMailException e, Model model) {
        e.printStackTrace();
        model.addAttribute("message",e.getMessage());
        return "message";
    }
}
