package com.iprody.payment.service;

import com.iprody.common.PageUtils;
import com.iprody.common.Pagination;
import com.iprody.common.ResultList;
import com.iprody.common.Sorting;
import com.iprody.payment.model.Payment;
import com.iprody.payment.repository.PaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepo paymentRepo;

    public Payment findById(UUID id) {
        return paymentRepo.findById(id).orElse(null);
    }

    public Payment save(Payment payment) {
        return paymentRepo.save(payment);
    }

    @Transactional(readOnly = true)
    public ResultList<Payment> findPageByFilter(PaymentFilter filter, Pagination pagination, Sorting sorting) {
        return ResultList.from(paymentRepo.findAllByFilter(
                filter.getDateRange().getFrom(),
                filter.getDateRange().getTo(),
                filter.getId(),
                filter.getInquiryRefId(),
                filter.getStatus(),
                PageUtils.of(pagination, sorting))
        );
    }
}
