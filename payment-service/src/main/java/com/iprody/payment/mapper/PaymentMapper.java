package com.iprody.payment.mapper;

import com.iprody.common.kafka.PaymentRequest;
import com.iprody.payment.model.payment.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PaymentMapper {
    PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);

    Payment toPaymentRequest(PaymentRequest paymentRequest);
}
