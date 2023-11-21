package org.tvr.YourCalendar.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvr.YourCalendar.models.Action;
import org.tvr.YourCalendar.models.Person;
import org.tvr.YourCalendar.repositories.ActionRepository;
import org.tvr.YourCalendar.repositories.PeopleRepository;
import org.tvr.YourCalendar.security.PersonDetails;
import org.tvr.YourCalendar.utils.CalendarWrap;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ActionService {
    private final ActionRepository actionRepository;
    private final PeopleRepository peopleRepository;
    private static final long DAY_TO_MS=86400000;
    private static final int YEAR=365;
    private static final int HALF_YEAR=180;
    private static final int MONTH=30;
    private Map<Integer,Set<Long>> doneAction;
    @Autowired
    private SessionService sessionService;
    @Autowired
    public ActionService(ActionRepository actionRepository, PeopleRepository peopleRepository) {
        this.actionRepository = actionRepository;
        this.peopleRepository = peopleRepository;
        doneAction=new ConcurrentHashMap<Integer,Set<Long>>();
    }
    @Transactional
    public void save(Action action,Person person) {
        actionRepository.save(action);
        person.getItems().add(action);
        sessionService.forChangeAllSession(new PersonDetails(person));
    }

    public List<Action> getActionsForThisDay(Person person, CalendarWrap calendar) {
        return person.getItems().stream().filter(action->
        {
            if (action.getRepeat()!=null) {
                return (calendar.toLocalDate().equals(action.getDate().toLocalDate())&&(!excludeDone(action.getId(), calendar.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())))
                        ||(checkRepeat(calendar.toLocalDate(), action));
            }
            else {
                return calendar.toLocalDate().equals(action.getDate().toLocalDate());
            }
        }).sorted(Comparator.comparing(Action::getDate)).toList();
    }
    private boolean checkRepeat(LocalDate calendar, Action action){
        if (calendar.isBefore(action.getDate().toLocalDate())) {
            return false;
        }
        long repeat;
        long dateForChek = calendar.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        switch (action.getRepeat()) {
            case YEAR -> {
                if (calendar.getDayOfMonth() == action.getDate().getDayOfMonth() &&
                        calendar.getMonthValue() == action.getDate().getMonthValue()) {
                    return !excludeDone(action.getId(), dateForChek);
                }
                else {
                    return false;
                }
            }
            case HALF_YEAR -> {
                if (calendar.getDayOfMonth() == action.getDate().getDayOfMonth() &&
                        calendar.getMonthValue() == action.getDate().getMonthValue()) {
                    return !excludeDone(action.getId(), dateForChek);
                }
                int repeatForYear = calendar.lengthOfYear() / 2;
                repeat = repeatForYear * DAY_TO_MS;
            }
            case MONTH -> {
                if (calendar.getDayOfMonth() == action.getDate().getDayOfMonth()) {
                    return !excludeDone(action.getId(), dateForChek);
                }
                else {
                    return false;
                }
            }
            default -> {
                repeat = action.getRepeat() * DAY_TO_MS;
            }
        }
        long nowCalendar = calendar.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long dateAction = action.getDate().toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        return ((nowCalendar-dateAction)%repeat==0)&&(!excludeDone(action.getId(), dateForChek));
    }
    public Set<Integer> getActionDayForMonth(Person person, CalendarWrap calendar) {
        if (person==null) {
            return Collections.emptySet();
        }
        Set<Integer> actionInMonthRepeat = new HashSet<>();
        Set<Integer> actionInMonth = person.getItems().stream().filter(action -> {
            if (action.getRepeat()==null) {
                return action.getDate().getMonthValue()== calendar.getMonth()+1;
            }
            else {
                return checkRepeatForMonth(action,calendar.toLocalDate(),actionInMonthRepeat);
            }
        }).map(action -> action.getDate().getDayOfMonth()).collect(Collectors.toSet());
        actionInMonth.addAll(actionInMonthRepeat);
        return actionInMonth;
    }

    private boolean checkRepeatForMonth(Action action, LocalDate calendar,Set<Integer> actionInMonthRepeat) {
        if (calendar.isBefore(action.getDate().toLocalDate())) {
            return false;
        }
        long dayForCheck = calendar.withDayOfMonth(action.getDate().getDayOfMonth()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long repeat;
        switch (action.getRepeat()) {
            case YEAR -> {
                    return (calendar.getMonthValue() == action.getDate().getMonthValue())&&
                            (!excludeDone(action.getId(), dayForCheck));
            }
            case HALF_YEAR -> {
                if (calendar.getMonthValue() == action.getDate().getMonthValue()) {
                    return !excludeDone(action.getId(), dayForCheck);
                }
                int repeatForYear = calendar.lengthOfYear() / 2;
                repeat = repeatForYear * DAY_TO_MS;
            }
            case MONTH -> {
                return !excludeDone(action.getId(), dayForCheck);
            }
            default -> {
                repeat = action.getRepeat() * DAY_TO_MS;
            }
        }
        long nowCalendar = calendar.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long dateAction = action.getDate().toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long monthMS = calendar.getDayOfMonth() * DAY_TO_MS;
         while (dateAction<=nowCalendar) {
             if (nowCalendar-dateAction<monthMS) {
                 Instant instant = Instant.ofEpochMilli(dateAction);
                 LocalDate localDate = instant.atZone(ZoneOffset.UTC).toLocalDate();
                 if(!excludeDone(action.getId(), dateAction)){
                     actionInMonthRepeat.add(localDate.getDayOfMonth());
                 }
             }
             dateAction+=repeat;
         }
        return false;
    }

    private boolean excludeDone(int id,long date){
        if (!doneAction.containsKey(id)) {
            return false;
        }
        else {
            return doneAction.get(id).contains(date);
        }
    }
    @Transactional
    public void done(int id,LocalDate exludeDate,Authentication authentication) {
        Action action = actionRepository.findById(id).orElse(null);
        if (action!=null) {
            Person person = peopleRepository.findById(action.getOwner().getId()).orElse(null);
            if (action.getRepeat()==null) {
                person.getItems().removeIf(act -> act.getId()==id);
                actionRepository.delete(action);
            }
            else {
                    if (doneAction.containsKey(id)) {
                        doneAction.get(id).add(exludeDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
                    }
                    else {
                        doneAction.put(id,new CopyOnWriteArraySet<>(Collections.singletonList(exludeDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())));
                    }
            }
            sessionService.forChangeAllSession(new PersonDetails(person));
        }
    }
    @Transactional
    public void delete(int id) {
        Action action = actionRepository.findById(id).orElse(null);
        if (action!=null){
            Person person = peopleRepository.findById(action.getOwner().getId()).orElse(null);
            person.getItems().removeIf(act -> act.getId()==id);
            actionRepository.delete(action);
            sessionService.forChangeAllSession(new PersonDetails(person));
        }
        clearDoneAction(id);

    }
    private void clearDoneAction(int id) {
        doneAction.remove(id);
    }

    public Action findById(int id) {
        return actionRepository.findById(id).orElse(null);
    }
    @Transactional
    public void editAllAction(Action action, Action forChange, Person person) {
        action.setId(forChange.getId());
        action.setOwner(forChange.getOwner());
        actionRepository.save(action);
        clearDoneAction(action.getId());
        person.getItems().removeIf(act ->act.getId()==action.getId());
        person.getItems().add(action);
        sessionService.forChangeAllSession(new PersonDetails(person));
    }

    public String repeatTextForModel(Integer repeat) {
        if (repeat==null) {
            return "Не повторять";
        }
        switch (repeat){
            case 365-> {
                return "Раз в год";
            }
            case 182-> {
                return "Раз в полгода";
            }
            case 30-> {
                return "Раз в месяц";
            }
            case 7-> {
                return "Раз в неделю";
            }
            default-> {
                if (repeat==1) {
                    return "Каждый день";
                }
                return "Раз в "+repeat+" дней (дня, день)";
            }
        }
    }
    @Transactional
    @Scheduled(cron = "0 0 3 1 * ?")
    public void clearActionMonthAgo(){
        List<Person> persons = peopleRepository.findAll();
        LocalDate now = LocalDate.now();
        persons.forEach(person ->{person.getItems().removeIf(action ->{
            if(now.isAfter(action.getDate().toLocalDate())) {
                if(action.getRepeat()==null) {
                    actionRepository.deleteById(action.getId());
                    return true;
                }
                else {
                    switch (action.getRepeat()) {
                        case 365 -> {
                            action.setDate(action.getDate().plusYears(1));
                        }
                        case 182 -> {
                            action.setDate(action.getDate().plusMonths(6));
                        }
                        case 30 -> {
                            action.setDate(action.getDate().plusMonths(1));
                        }
                        default -> {
                            LocalDateTime nowTime = now.atStartOfDay();
                            LocalDateTime dateForAction = action.getDate();
                            while (dateForAction.isBefore(nowTime)){
                                dateForAction=dateForAction.plusDays(action.getRepeat());
                            }
                            action.setDate(dateForAction);
                        }
                    }
                    return false;
                }
            }
            return false;
        });
        sessionService.forChangeAllSession(new PersonDetails(person));
        });
    }
}

