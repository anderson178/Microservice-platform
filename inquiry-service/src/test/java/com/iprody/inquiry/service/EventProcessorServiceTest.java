package com.iprody.inquiry.service;

import com.iprody.inquiry.kafka.event.CancellationEventPublisher;
import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationResponse;
import com.iprody.common.kafka.CancellationStatus;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryStatus;
import com.iprody.inquiry.model.InquiryUpdateData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventProcessorService Unit Tests")
class EventProcessorServiceTest {
    @Mock
    private InquiryService inquiryService;

    @Mock
    private CancellationEventPublisher cancellationEventPublisher;

    @InjectMocks
    private EventProcessorService eventProcessorService;

    @Nested
    @DisplayName("processCancellationRequest()")
    class ProcessRequestTests {

        @Test
        @DisplayName("should check inquiry exists and publish to Kafka")
        void processCancellationRequest_validInquiry_publishesEvent() {
            UUID inquiryId = UUID.randomUUID();
            CancellationRequest request = new CancellationRequest();
            request.setId(inquiryId);
            request.setReason("Customer request");

            eventProcessorService.processCancellationRequest(request);

            verify(inquiryService).checkById(inquiryId);
            verify(cancellationEventPublisher).publish(request);
        }

        @Test
        @DisplayName("should propagate exception if inquiry not found")
        void processCancellationRequest_notFound_propagatesException() {
            CancellationRequest request = new CancellationRequest();
            request.setId(UUID.randomUUID());

            doThrow(new RuntimeException("Inquiry not found"))
                    .when(inquiryService).checkById(request.getId());

            assertThatThrownBy(() -> eventProcessorService.processCancellationRequest(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Inquiry not found");

            verify(cancellationEventPublisher, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("processCancellationResponse()")
    class ProcessResponseTests {

        @Test
        @DisplayName("should set CANCELLED status when response is SUCCESS")
        void processCancellationResponse_success_setsCancelledStatus() {
            UUID inquiryId = UUID.randomUUID();
            Inquiry inquiry = new Inquiry();
            inquiry.setId(inquiryId);
            inquiry.setStatus(InquiryStatus.IN_PROGRESS);
            inquiry.setNote("Old note");

            CancellationResponse response = new CancellationResponse();
            response.setId(inquiryId);
            response.setStatus(CancellationStatus.SUCCESS);
            response.setReason("Approved by manager");

            Inquiry updatedInquiry = new Inquiry();
            updatedInquiry.setId(inquiryId);
            updatedInquiry.setStatus(InquiryStatus.CANCELLED);
            updatedInquiry.setNote("Cancelled via external service. Reason: Approved by manager");
            updatedInquiry.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

            when(inquiryService.findById(inquiryId)).thenReturn(inquiry);

            when(inquiryService.update(eq(inquiryId), any(InquiryUpdateData.class)))
                    .thenReturn(updatedInquiry);

            eventProcessorService.processCancellationResponse(response);

            verify(inquiryService).update(eq(inquiryId), argThat(
                    data -> data != null && data.getStatus() == InquiryStatus.CANCELLED
            ));
        }

        @Test
        @DisplayName("should propagate exception if inquiry not found")
        void processCancellationResponse_notFound_propagatesException() {
            CancellationResponse response = new CancellationResponse();
            response.setId(UUID.randomUUID());

            when(inquiryService.findById(response.getId())).thenReturn(null);

            assertThatThrownBy(() -> eventProcessorService.processCancellationResponse(response))
                    .isInstanceOf(NullPointerException.class);

            verify(inquiryService, never()).update(any(), any());
        }
    }
}
