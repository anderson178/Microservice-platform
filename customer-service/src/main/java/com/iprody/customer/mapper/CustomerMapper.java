package com.iprody.customer.mapper;

import com.iprody.common.CommonMapper;
import com.iprody.common.ResultList;
import com.iprody.common.Sorting;
import com.iprody.common.dto.SortingDto;
import com.iprody.common.utils.EnumUtils;
import com.iprody.customer.dto.CustomerDataDto;
import com.iprody.customer.dto.CustomerDto;
import com.iprody.customer.dto.CustomerFilterDto;
import com.iprody.customer.dto.CustomerSortFieldDto;
import com.iprody.customer.model.Customer;
import com.iprody.customer.model.CustomerData;
import com.iprody.customer.model.CustomerFilter;
import com.iprody.customer.model.CustomerSortField;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Sort;

@Mapper
public interface CustomerMapper {
    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    CustomerDto toDto(Customer customer);

    @Mapping(target = "contract.email", source = "contract.email")
    @Mapping(target = "contract.phoneNumber", source = "contract.phoneNumber")
    CustomerData toData(CustomerDataDto dto);

    @BeanMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    CustomerFilter toFilter(CustomerFilterDto dto);

    default Sorting toSorting(SortingDto<CustomerSortFieldDto> sortingDto) {
        if (sortingDto == null) {
            return new Sorting(CustomerSortField.FULL_NAME, Sort.Direction.DESC);
        }

        CustomerSortField sortField = EnumUtils.getEnum(
                sortingDto.getSortField(),
                CustomerSortField.class,
                CustomerSortField.FULL_NAME
        );
        Sort.Direction direction = CommonMapper.INSTANCE.toDirection(sortingDto.getSortDirection());

        return new Sorting(sortField, direction != null ? direction : Sort.Direction.DESC);
    }

    ResultList<CustomerDto> toDtoList(ResultList<Customer> customerResultList);
}
