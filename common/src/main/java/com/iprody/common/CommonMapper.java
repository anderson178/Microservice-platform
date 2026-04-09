package com.iprody.common;

import com.iprody.common.dto.PaginationDto;
import com.iprody.common.dto.SortDirectionTypeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Sort;

@Mapper
public interface CommonMapper {
    CommonMapper INSTANCE = Mappers.getMapper(CommonMapper.class);

    Pagination toPagination(PaginationDto dto);

    Sort.Direction toDirection(SortDirectionTypeDto dto);
}
