package com.iprody.customer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "CONTRACT")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "EMAIL", nullable = false)
    private String email;

    @Column(name = "PHONE_NUMBER", nullable = false)
    private String phoneNumber;

    @Generated(event = EventType.INSERT)
    @Column(name = "CREATED_AT", nullable = false)
    private Timestamp createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UPDATED_AT", nullable = false)
    private Timestamp updatedAt;


    public Contract(String email, String phoneNumber) {
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
}
