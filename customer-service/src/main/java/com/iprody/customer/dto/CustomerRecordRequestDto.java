package com.iprody.customer.dto;

import com.iprody.common.dto.AbstractRecordRequestDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CustomerRecordRequestDto extends AbstractRecordRequestDto<CustomerSortFieldDto> {
    private CustomerFilterDto filter;
}
