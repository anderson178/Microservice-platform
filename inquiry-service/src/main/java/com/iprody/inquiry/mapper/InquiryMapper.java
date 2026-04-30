package com.iprody.inquiry.mapper;

import com.iprody.common.CommonMapper;
import com.iprody.common.ResultList;
import com.iprody.common.Sorting;
import com.iprody.common.dto.SortingDto;
import com.iprody.common.utils.EnumUtils;
import com.iprody.inquiry.dto.*;
import com.iprody.common.kafka.CancellationRequest;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryData;
import com.iprody.inquiry.model.InquirySortField;
import com.iprody.inquiry.model.InquiryUpdateData;
import com.iprody.inquiry.service.InquiryFilter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Sort;

@Mapper
public interface InquiryMapper {
    InquiryMapper INSTANCE = Mappers.getMapper(InquiryMapper.class);

    Inquiry fromData(InquiryData data);

    @Mapping(target = "status", source = "data.status")
    @Mapping(target = "managerRefId", source = "data.managerRefId")
    @Mapping(target = "note", source = "data.note")
    Inquiry update(Inquiry inquiry, InquiryUpdateData data);

    InquiryDto toDto(Inquiry inquiry);

    InquiryData toData(InquiryDataDto dto);

    ResultList<InquiryDto> toDtoList(ResultList<Inquiry> customerResultList);

    @BeanMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    InquiryFilter toFilter(InquiryFilterDto dto);

    default Sorting toSorting(SortingDto<InquirySortFieldDto> sortingDto) {
        if (sortingDto == null) {
            return new Sorting(InquirySortField.CREATED_AT, Sort.Direction.DESC);
        }

        InquirySortField sortField = EnumUtils.getEnum(
                sortingDto.getSortField(),
                InquirySortField.class,
                InquirySortField.CREATED_AT
        );
        Sort.Direction direction = CommonMapper.INSTANCE.toDirection(sortingDto.getSortDirection());

        return new Sorting(sortField, direction != null ? direction : Sort.Direction.DESC);
    }

    CancellationRequest fromCancellationRequestDto(CancellationRequestDto dto);

    InquiryUpdateData update(Inquiry inquiry);
}
