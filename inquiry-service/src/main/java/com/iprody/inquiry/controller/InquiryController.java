package com.iprody.inquiry.controller;

import com.iprody.common.CommonMapper;
import com.iprody.common.ResultList;
import com.iprody.inquiry.dto.InquiryDataDto;
import com.iprody.inquiry.dto.InquiryDto;
import com.iprody.inquiry.dto.InquiryRecordRequestDto;
import com.iprody.inquiry.mapper.InquiryMapper;
import com.iprody.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = "API for inquires")
@ApiResponses(
        value = {
                @ApiResponse(responseCode = "400", description = "Bad request"),
                @ApiResponse(responseCode = "404", description = "Not found"),
                @ApiResponse(responseCode = "424", description = "External service unavailable"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/v1/inquires", produces = MediaType.APPLICATION_JSON_VALUE)
public class InquiryController {
    private final InquiryService inquiryService;

    @PostMapping
    public InquiryDto save(@Valid @RequestBody InquiryDataDto dto) {
        return InquiryMapper.INSTANCE.toDto(
                inquiryService.save(
                        InquiryMapper.INSTANCE.toData(dto)
                )
        );
    }

    @GetMapping("/search")
    public ResultList<InquiryDto> findAllByFilter(InquiryRecordRequestDto inquiryRecordRequestDto) {
        return InquiryMapper.INSTANCE.toDtoList(
                inquiryService.findAllByFilter(
                        InquiryMapper.INSTANCE.toFilter(inquiryRecordRequestDto.getFilter()),
                        CommonMapper.INSTANCE.toPagination(inquiryRecordRequestDto.getPagination()),
                        InquiryMapper.INSTANCE.toSorting(inquiryRecordRequestDto.getSorting())
                )
        );
    }
}
