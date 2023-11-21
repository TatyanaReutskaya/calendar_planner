package org.tvr.YourCalendar.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Entity
@Table(name = "action")
public class Action{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    @NotEmpty(message = "Название события должно быть указано.")
    @Size(max = 200, min = 2, message = "Название события должно содержать не менее 2 и не более 200 символов.")
    private String name;

    @Column(name = "comment")
    @Size(max = 200, message = "Комментарий должен содержать не более 200 символов")
    private String comment;

    @Column(name = "repeat_date")
    @ColumnDefault("0")
    @Min(value = 1, message="Повтор возможен не чаще 1 раза в день")
    @Max(value = 366, message="Повтор возможен не реже 1 раза в год")
    private Integer repeat;

    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)
    /*@NotNull(message = "Date shouldn`t be empty")*/
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "person_id",referencedColumnName = "id")
    private Person owner;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return id == action.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Action{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", repeat=" + repeat +
                ", date=" + date +
                '}';
    }
}
