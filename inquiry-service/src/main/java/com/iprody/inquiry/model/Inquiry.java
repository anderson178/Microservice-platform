package com.iprody.inquiry.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Entity
@Table(name = "inquiry")
public class Inquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_ref_id", nullable = false)
    private UUID customerRefId;

    @Column(name = "group_ref_id", nullable = false)
    private UUID groupRefId;

    @Column(name = "manager_ref_id", nullable = false)
    private UUID managerRefId;

    // max-length 100)
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "comment")
    private String comment;

    // from inquiry
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InquiryStatus status;

    // for manager while working on inquiry
    @Column(name = "note")
    private String note;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "number_of_seats")
    private Long numberOfSeats;
}
