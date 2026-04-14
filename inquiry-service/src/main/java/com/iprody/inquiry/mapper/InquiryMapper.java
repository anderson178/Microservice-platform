package com.iprody.inquiry.mapper;

import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryData;
import com.iprody.inquiry.model.InquiryUpdateData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface InquiryMapper {
    InquiryMapper INSTANCE = Mappers.getMapper(InquiryMapper.class);

    Inquiry fromData(InquiryData data);

    @Mapping(target = "status", source = "data.status")
    @Mapping(target = "managerRefId", source = "data.managerRefId")
    Inquiry update(Inquiry inquiry, InquiryUpdateData data);
}
