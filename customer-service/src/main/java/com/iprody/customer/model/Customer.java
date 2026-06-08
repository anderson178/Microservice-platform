package com.iprody.customer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "CUSTOMER")
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "FULL_NAME", nullable = false)
    private String fullName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "CONTRACT_DETAILS_IS", referencedColumnName = "id")
    private Contract contract;

    @Generated(event = EventType.INSERT)
    @Column(name = "CREATED_AT", nullable = false)
    private Timestamp createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UPDATED_AT", nullable = false)
    private Timestamp updatedAt;

    public Customer(String fullName, Contract contract) {
        this.fullName = fullName;
        this.contract = contract;
    }
}
