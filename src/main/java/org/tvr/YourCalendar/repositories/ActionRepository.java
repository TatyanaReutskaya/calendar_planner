package org.tvr.YourCalendar.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvr.YourCalendar.models.Action;

public interface ActionRepository extends JpaRepository<Action,Integer> {
    public void deleteById(int id);
}
