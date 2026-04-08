package com.iprody.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_ref_id", nullable = false)
    private UUID groupRefId;

    @Column(name = "current_count")
    private Long currentCount;

    @Column(name = "limit_int")
    private Long limit;
}
