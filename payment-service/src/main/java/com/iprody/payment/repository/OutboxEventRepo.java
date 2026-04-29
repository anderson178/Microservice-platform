package com.iprody.payment.repository;

import com.iprody.payment.model.outbox.OutboxEvent;
import com.iprody.payment.model.outbox.OutboxEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepo extends JpaRepository<OutboxEvent, UUID> {
    @Query("""
            select e from OutboxEvent e
            where e.status = 'PENDING'
            order by e.createdAt asc
            """)
    List<OutboxEvent> findPendingEvents(Pageable pageable);

    boolean existsByAggregateIdAndEventType(UUID aggregateId, OutboxEventType type);

    List<OutboxEvent> findByAggregateIdAndEventType(UUID aggregateId, OutboxEventType type);
}
