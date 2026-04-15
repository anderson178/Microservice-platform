package com.iprody.inquiry.service;

import com.iprody.common.*;
import com.iprody.common.exception.AppException;
import com.iprody.inquiry.mapper.InquiryMapper;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryData;
import com.iprody.inquiry.model.InquiryStatus;
import com.iprody.inquiry.model.InquiryUpdateData;
import com.iprody.inquiry.repository.InquiryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private final InquiryRepo inquiryRepo;

    @Transactional
    public Inquiry save(InquiryData data) {
        Inquiry inquiry = InquiryMapper.INSTANCE.fromData(data);
        inquiry.setStatus(InquiryStatus.NEW);
        return inquiryRepo.save(inquiry);
    }

    @Transactional(readOnly = true)
    public Inquiry findById(UUID id) {
        return inquiryRepo.findById(id).orElseThrow(() ->  new AppException(ResultCode.NOT_FOUND));
    }

    @Transactional
    public Inquiry update(UUID id, InquiryUpdateData data) {
        return inquiryRepo.save(
                InquiryMapper.INSTANCE.update(
                        findById(id),
                        data
                )
        );
    }

    @Transactional(readOnly = true)
    public ResultList<Inquiry> findAllByFilter(InquiryFilter filter, Pagination pagination, Sorting sorting) {
        return ResultList.from(inquiryRepo.findAllByFilter(
                filter.getStatus(),
                filter.getCustomerRefId(),
                filter.getManagerRefId(),
                PageUtils.of(pagination, sorting))
        );
    }
}
