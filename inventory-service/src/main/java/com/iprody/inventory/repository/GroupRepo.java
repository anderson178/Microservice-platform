package com.iprody.inventory.repository;

import com.iprody.inventory.model.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepo extends JpaRepository<Group, UUID> {
    @Query("""
            select g from Group g
            where(:groupRefId is null or g.groupRefId = :groupRefId)
            and (
                 :isAvailFreePlaces is null
                 or (:isAvailFreePlaces = true and g.currentCount < g.limit)
                 or (:isAvailFreePlaces = false and g.currentCount >= g.limit)
             )
            """)
    Page<Group> findAllByFilter(
            @Param("groupRefId") UUID groupRefId,
            @Param("isAvailFreePlaces") Boolean isAvailFreePlaces,
            Pageable pageable
    );

    @Modifying
    @Query("update Group g set g.currentCount = g.currentCount - 1 where g.groupRefId = :groupRefId and g.currentCount > 0")
    int decrementCountByGroupRefIdId(@Param("groupRefId") UUID groupRefId);

    @Modifying
    @Query("""
            UPDATE Group g SET g.currentCount = g.currentCount + :requested
            WHERE g.groupRefId = :groupRefId AND (g.currentCount + :requested <= g.limit)
            """)
    int increase(@Param("groupRefId") UUID groupRefId,
                 @Param("count") Long count);

    Optional<Group> findByGroupRefId(UUID groupRefId);
}
