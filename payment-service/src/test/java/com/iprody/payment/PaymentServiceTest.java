package com.iprody.payment;


import com.iprody.common.DateRange;
import com.iprody.common.Pagination;
import com.iprody.common.ResultList;
import com.iprody.common.Sorting;
import com.iprody.payment.model.Payment;
import com.iprody.payment.model.PaymentSortField;
import com.iprody.payment.repository.PaymentRepo;
import com.iprody.payment.service.PaymentFilter;
import com.iprody.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService tests")
public class PaymentServiceTest {
    @Mock
    private PaymentRepo paymentRepo;

    @InjectMocks
    private PaymentService paymentService;


    @Test
    @DisplayName("Should successfully return paginated payments by filter")
    void findPageByFilter_Success() {
        Page<Payment> mockPage = new PageImpl<>(List.of(new Payment()), PageRequest.of(0, 10), 1);
        when(paymentRepo.findAllByFilter(any(), any(), any(), any(), any(), any())).thenReturn(mockPage);
        ResultList<Payment> result = paymentService.findPageByFilter(
                getFilter(),
                new Pagination(0, 10),
                new Sorting(PaymentSortField.STATUS, Sort.Direction.DESC));
        assertThat(result.getElements()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
        verify(paymentRepo, times(1)).findAllByFilter(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return empty list when no payments found")
    void findPageByFilter_EmptyResult() {
        when(paymentRepo.findAllByFilter(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
        ResultList<Payment> result = paymentService.findPageByFilter(
                getFilter(),
                new Pagination(0, 10),
                new Sorting(PaymentSortField.STATUS, Sort.Direction.DESC));
        assertThat(result.getElements()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
    }

    private PaymentFilter getFilter() {
        PaymentFilter filter = new PaymentFilter();
        filter.setDateRange(new DateRange());

        return filter;
    }
}
