package com.iprody.payment.repository;

import com.iprody.payment.model.Payment;
import com.iprody.payment.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, UUID> {

    @Query(
            """
                    select p from Payment p
                    where p.createdAt >= coalesce(:from, p.createdAt)
                    and p.createdAt <= coalesce(:to, p.createdAt)
                    and (:id is null or p.id = :id)
                    and (:inquiryRefId is null or p.inquiryRefId = :inquiryRefId)
                    and (:paymentStatus is null or p.paymentStatus = :paymentStatus)
                    """)
    Page<Payment> findAllByFilter(
            @Param("from") Timestamp filterFrom,
            @Param("to") Timestamp filterTo,
            @Param("id") UUID id,
            @Param("inquiryRefId") UUID inquiryRefId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable
    );
}
