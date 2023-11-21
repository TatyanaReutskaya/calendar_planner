package org.tvr.YourCalendar.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

@Component
@SessionScope
public class CalendarWrap {
    @Setter
    @Getter
    private Calendar calendar;
    private final String[] months;
    public CalendarWrap() {
        calendar = Calendar.getInstance();
        months = new String[]{"Январь", "Февраль", "Март",
                "Апрель", "Май","Июнь",
                "Июль", "Август","Сентябрь",
                "Октябрь", "Ноябрь", "Декабрь"};
    }
    public String getMonthString() {
        /*return calendar.getDisplayName(Calendar.MONTH,Calendar.LONG_FORMAT, Locale.getDefault()).toUpperCase();*/
        return months[calendar.get(Calendar.MONTH)];
    }
    public String getFoolDate(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        return dateFormat.format(calendar.getTime());
    }
    public LocalDate toLocalDate(){
        return calendar.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    public int getMonth(){
        return calendar.get(Calendar.MONTH);
    }
    public int getYear() {
        return calendar.get(Calendar.YEAR);
    }
    public int getDay() {
        return calendar.get(Calendar.DAY_OF_MONTH);
    }
    public void monthUp() {
        calendar.add(Calendar.MONTH,1);
    }
    public void monthDown() {
        calendar.add(Calendar.MONTH,-1);
    }
    public void yearUp() {
        calendar.add(Calendar.YEAR,1);
    }
    public void yearDown() {
        calendar.add(Calendar.YEAR,-1);
    }
    public int[] setupDaysInMonth() {
        int[] daysInMonth = new int[42];
        int nowDay = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH,1);
        int calendarDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int dayOfWeekForArray = calendarDayOfWeek!=1?calendarDayOfWeek-2:calendarDayOfWeek+5;
        calendar.set(Calendar.DAY_OF_MONTH,nowDay);
        calendar.add(Calendar.MONTH,-1);
        int dayInPrevMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int countDay=0;
        if (dayOfWeekForArray!=0) {
            for (int i = dayOfWeekForArray-1;i>=0;i--) {
                daysInMonth[i]=dayInPrevMonth-countDay;
                countDay++;
            }
        }
        countDay=1;
        calendar.add(Calendar.MONTH,1);
        for (int i = dayOfWeekForArray;i<calendar.getActualMaximum(Calendar.DAY_OF_MONTH)+dayOfWeekForArray;i++) {
            daysInMonth[i]=countDay;
            countDay++;
        }
        countDay=1;
        for (int i = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)+dayOfWeekForArray;i<daysInMonth.length;i++) {
            daysInMonth[i]=countDay;
            countDay++;
        }
        return daysInMonth;
    }
    public void setLastDayMonth(){
        calendar.set(Calendar.DAY_OF_MONTH,calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    }
    public boolean isNow(int day) {
        LocalDate now = LocalDate.now();
        LocalDate show = toLocalDate().withDayOfMonth(day);
        return now.equals(show);
    }
    public void setDay(int day) {
        calendar.set(Calendar.DAY_OF_MONTH,day);
    }

}
