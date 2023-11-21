package org.tvr.YourCalendar.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.tvr.YourCalendar.models.Person;

import java.util.Optional;

@Repository
public interface PeopleRepository extends JpaRepository<Person,Integer> {
    Optional<Person> findByUsername(String username);
    Optional<Person> findByEmail(String email);
    @Modifying
    @Query("DELETE FROM Person p WHERE p.enable=false")
    void cleanNotEnabled();
}
