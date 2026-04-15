package com.iprody.inquiry.dto;

import com.iprody.common.dto.AbstractRecordRequestDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class InquiryRecordRequestDto extends AbstractRecordRequestDto<InquirySortFieldDto> {
    private InquiryFilterDto filter;
}
