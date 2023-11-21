package org.tvr.YourCalendar.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.Cascade;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "person")
public class Person {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "username")
    @NotEmpty(message = "Поле не должно быть пустым.")
    @Size(max = 15, min = 2, message = "Имя должно содержать от 2 до 15 символов")
    private String username;

    @Column(name = "email")
    @NotEmpty(message = "Email shouldn`t be empty")
    @Email
    private String email;

    @Column(name = "date_of_birth")
    @Temporal(TemporalType.DATE)
    @Past(message = "Date of Birh should be valid")
    @NotNull(message = "Date of Birh shouldn`t be empty")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Column(name = "password",length = 200)
    //добавить not empty и при редактировании присваивать существующий?
    private String password;

    @OneToMany (mappedBy = "owner",fetch = FetchType.EAGER,cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<Action> items;

    @Column(name = "chat_id")
    private Long chatId;

    @Column (name = "enable")
    private boolean enable;

    @Override
    public String toString() {
        return "Person{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", enable=" + enable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return id == person.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
