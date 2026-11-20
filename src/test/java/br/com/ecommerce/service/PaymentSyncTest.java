package br.com.ecommerce.service;

import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.PaymentRepository;
import br.com.ecommerce.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSyncTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PurchaseOrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private PurchaseOrder order;
    private Payment payment;

    @BeforeEach
    void setup() {
        user = new User();
        user.id = "user-123";

        order = new PurchaseOrder();
        order.id = "order-xyz";
        order.user = user;
        order.status = OrderStatus.PENDING;

        payment = new Payment();
        payment.id = "pay-1";
        payment.order = order;
        payment.status = "PENDING";
    }

    @Test
    @DisplayName("FR-006: Cancelar pagamento deve sincronizar cancelamento do pedido via OrderService")
    void cancelPaymentSynchronizesOrderCancellation() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.cancel("pay-1", "user-123");

        assertEquals("CANCELED", payment.status);
        verify(orderService).cancel(eq("order-xyz"), eq("user-123"), eq(false));
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Cancelar pagamento já confirmado deve lançar 409 Conflict")
    void cancelPaidPaymentThrowsConflict() {
        payment.status = "PAID";
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));

        ApiException ex = assertThrows(ApiException.class, () ->
                paymentService.cancel("pay-1", "user-123")
        );

        assertEquals(HttpStatus.CONFLICT, ex.status);
        verify(orderService, never()).cancel(any(), any(), anyBoolean());
    }
}
