package com.iprody.inventory.repository;

import com.iprody.inventory.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepo extends JpaRepository<OutboxEvent, Long> {
    @Query("""
            select e from OutboxEvent e
            where e.status = 'PENDING'
            order by e.createdAt asc
            """)
    List<OutboxEvent> findPendingEvents(Pageable pageable);
}
