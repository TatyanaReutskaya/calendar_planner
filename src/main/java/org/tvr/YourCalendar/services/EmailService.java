package org.tvr.YourCalendar.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.tvr.YourCalendar.models.Person;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    private final EncodeDecodeServise encodeDecodeServise;
    private final JavaMailSender emailSender;
    @Autowired
    public EmailService(EncodeDecodeServise encodeDecodeServise, JavaMailSender emailSender) {
        this.encodeDecodeServise = encodeDecodeServise;
        this.emailSender = emailSender;
    }
    public void sendMailForFinishRegistration(Person person) {
        LocalDate date = LocalDate.now();
        date = date.plusDays(1);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d.MM.y");
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(person.getEmail());
        simpleMailMessage.setSubject("Завершение регистрации в приложении YourCalendar");
        String message = "Благодарю за регистрацию в приложении YourCalendar. Для завершения регистрации перейдите по ссылке: "
                +"http://localhost:8080/registration/"
                + encodeDecodeServise.encode(person.getEmail())
                + "\n Ссылка действительна до 00:00 "+date.format(dateTimeFormatter)+", по истечении времени действия ссылки регистрацию будет нужно пройти повторно.";
        simpleMailMessage.setText(message);
        emailSender.send(simpleMailMessage);
    }
    public void sendMailForUpdateEmail(String oldEmail,String newEmail) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(newEmail);
        simpleMailMessage.setSubject("Изменеие email в приложении YourCalendar");
        String message = "Для изменения email перейдите по ссылке: "
                +"http://localhost:8080/changeEmail/"
                + encodeDecodeServise.encode(oldEmail+"|"+newEmail)
                +"\n Если вы не меняли email просто проигнорируйте это сообщение.";
        simpleMailMessage.setText(message);
        emailSender.send(simpleMailMessage);
        System.out.println("сообщение отправлено");
    }
    public void sendMailForRestorePassword(Person person){
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(person.getEmail());
        simpleMailMessage.setSubject("Восстановление пароля в приложении YourCalendar");
        String message = "Для восстановления пароля перейдите по ссылке: "
                +"http://localhost:8080/restorePassword/"
                + encodeDecodeServise.encode(person.getEmail()+"|"+person.getPassword());
        simpleMailMessage.setText(message);
        emailSender.send(simpleMailMessage);
    }
}
