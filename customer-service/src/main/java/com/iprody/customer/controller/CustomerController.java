package com.iprody.customer.controller;

import com.iprody.common.CommonMapper;
import com.iprody.common.ResultList;
import com.iprody.customer.dto.CustomerDataDto;
import com.iprody.common.dto.CustomerDto;
import com.iprody.customer.dto.CustomerRecordRequestDto;
import com.iprody.customer.mapper.CustomerMapper;
import com.iprody.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "API for customers")
@ApiResponses(
        value = {
                @ApiResponse(responseCode = "400", description = "Bad request"),
                @ApiResponse(responseCode = "404", description = "Not found"),
                @ApiResponse(responseCode = "424", description = "External service unavailable"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/v1/customers", produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public CustomerDto getById(@PathVariable UUID id) {
        return CustomerMapper.INSTANCE.toDto(customerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerDto save(@Valid @RequestBody CustomerDataDto dto) {
        return CustomerMapper.INSTANCE.toDto(
                customerService.save(
                        CustomerMapper.INSTANCE.toData(dto)
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public CustomerDto update(@PathVariable UUID id,
                              @Valid @RequestBody CustomerDataDto dto) {
        return CustomerMapper.INSTANCE.toDto(
                customerService.update(
                        id,
                        CustomerMapper.INSTANCE.toData(dto)
                )
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResultList<CustomerDto> findAllByFilter(CustomerRecordRequestDto customerRecordRequestDto) {
        return CustomerMapper.INSTANCE.toDtoList(
                customerService.findAllByFilter(
                        CustomerMapper.INSTANCE.toFilter(customerRecordRequestDto.getFilter()),
                        CommonMapper.INSTANCE.toPagination(customerRecordRequestDto.getPagination()),
                        CustomerMapper.INSTANCE.toSorting(customerRecordRequestDto.getSorting())
                )
        );
    }
}
