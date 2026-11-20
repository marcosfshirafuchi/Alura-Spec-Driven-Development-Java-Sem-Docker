package br.com.ecommerce.service;

import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private PurchaseOrderRepository orders;

    @Mock
    private ProductRepository products;

    @Mock
    private UserRepository users;

    @Mock
    private CartService carts;

    @InjectMocks
    private OrderService orderService;

    private User owner;
    private Product product1;
    private Product product2;
    private PurchaseOrder order;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.id = "user-123";
        owner.name = "Cliente Titular";
        owner.email = "titular@email.com";
        owner.role = Role.USER;

        Category category = new Category();
        category.id = "cat-1";
        category.name = "Eletrônicos";

        product1 = new Product();
        product1.id = "prod-1";
        product1.name = "Mouse Gamer";
        product1.sku = "MOUSE-01";
        product1.price = BigDecimal.valueOf(100.0);
        product1.stock = 10;
        product1.status = ProductStatus.ACTIVE;
        product1.category = category;

        product2 = new Product();
        product2.id = "prod-2";
        product2.name = "Teclado Mecânico";
        product2.sku = "TECLADO-01";
        product2.price = BigDecimal.valueOf(250.0);
        product2.stock = 5;
        product2.status = ProductStatus.ACTIVE;
        product2.category = category;

        order = new PurchaseOrder();
        order.id = "order-abc";
        order.user = owner;
        order.status = OrderStatus.PENDING;
        order.totalValue = BigDecimal.valueOf(450.0);

        OrderItem item1 = new OrderItem();
        item1.id = UUID.randomUUID().toString();
        item1.order = order;
        item1.product = product1;
        item1.quantity = 2;
        item1.price = product1.price;
        order.items.add(item1);

        OrderItem item2 = new OrderItem();
        item2.id = UUID.randomUUID().toString();
        item2.order = order;
        item2.product = product2;
        item2.quantity = 1;
        item2.price = product2.price;
        order.items.add(item2);
    }

    @Test
    @DisplayName("FR-001/FR-002: Deve cancelar pedido PENDING e restaurar estoque de todos os itens")
    void shouldCancelOrderAndRestoreStockWhenStatusIsPending() {
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder canceled = orderService.cancel("order-abc", "user-123", false);

        assertNotNull(canceled);
        assertEquals(OrderStatus.CANCELED, canceled.status);
        assertEquals(12, product1.stock, "Estoque do prod-1 deve retornar de 10 para 12 (+2)");
        assertEquals(6, product2.stock, "Estoque do prod-2 deve retornar de 5 para 6 (+1)");
        verify(products).save(product1);
        verify(products).save(product2);
        verify(orders).save(order);
    }

    @Test
    @DisplayName("FR-001/FR-002: Deve cancelar pedido PAID e restaurar estoque de todos os itens")
    void shouldCancelOrderAndRestoreStockWhenStatusIsPaid() {
        order.status = OrderStatus.PAID;
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder canceled = orderService.cancel("order-abc", "user-123", false);

        assertNotNull(canceled);
        assertEquals(OrderStatus.CANCELED, canceled.status);
        assertEquals(12, product1.stock);
        assertEquals(6, product2.stock);
        verify(products).save(product1);
        verify(products).save(product2);
        verify(orders).save(order);
    }

    @Test
    @DisplayName("FR-001/FR-002: Deve cancelar pedido PACKING e restaurar estoque de todos os itens")
    void shouldCancelOrderAndRestoreStockWhenStatusIsPacking() {
        order.status = OrderStatus.PACKING;
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder canceled = orderService.cancel("order-abc", "user-123", false);

        assertNotNull(canceled);
        assertEquals(OrderStatus.CANCELED, canceled.status);
        assertEquals(12, product1.stock);
        assertEquals(6, product2.stock);
        verify(products).save(product1);
        verify(products).save(product2);
        verify(orders).save(order);
    }

    @Test
    @DisplayName("FR-003/US2: Deve rejeitar cancelamento com 400 Bad Request quando status for SHIPPED sem alterar estoque")
    void shouldThrowBadRequestWhenOrderIsShipped() {
        order.status = OrderStatus.SHIPPED;
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class, () ->
                orderService.cancel("order-abc", "user-123", false)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
        assertTrue(ex.getMessage().toLowerCase().contains("enviado") || ex.getMessage().toLowerCase().contains("shipped"));
        assertEquals(OrderStatus.SHIPPED, order.status, "Status do pedido não deve ser alterado");
        assertEquals(10, product1.stock, "Estoque não deve ser estornado");
        assertEquals(5, product2.stock, "Estoque não deve ser estornado");
        verify(products, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    @DisplayName("FR-003/US2: Deve rejeitar cancelamento com 400 Bad Request quando status for DELIVERED sem alterar estoque")
    void shouldThrowBadRequestWhenOrderIsDelivered() {
        order.status = OrderStatus.DELIVERED;
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class, () ->
                orderService.cancel("order-abc", "user-123", false)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.status);
        assertTrue(ex.getMessage().toLowerCase().contains("entregue") || ex.getMessage().toLowerCase().contains("enviado") || ex.getMessage().toLowerCase().contains("delivered"));
        assertEquals(OrderStatus.DELIVERED, order.status);
        assertEquals(10, product1.stock);
        assertEquals(5, product2.stock);
        verify(products, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    @DisplayName("FR-007/Edge Case: Deve ser idempotente quando o pedido já estiver CANCELED, sem duplicar estorno")
    void shouldBeIdempotentWhenOrderAlreadyCanceled() {
        order.status = OrderStatus.CANCELED;
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));

        PurchaseOrder result = orderService.cancel("order-abc", "user-123", false);

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELED, result.status);
        assertEquals(10, product1.stock, "Estoque não deve ser incrementado novamente");
        assertEquals(5, product2.stock, "Estoque não deve ser incrementado novamente");
        verify(products, never()).save(any());
    }

    @Test
    @DisplayName("FR-005: Deve lançar 403 Forbidden quando usuário requisitante não for o titular nem ADMIN")
    void shouldThrowForbiddenWhenUserIsNotOwnerAndNotAdmin() {
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class, () ->
                orderService.cancel("order-abc", "outro-usuario", false)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.status);
        assertEquals(OrderStatus.PENDING, order.status);
        assertEquals(10, product1.stock);
        verify(products, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    @DisplayName("FR-005: Deve permitir que usuário ADMIN cancele pedido de qualquer cliente")
    void shouldAllowAdminToCancelAnyOrder() {
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder canceled = orderService.cancel("order-abc", "admin-user", true);

        assertNotNull(canceled);
        assertEquals(OrderStatus.CANCELED, canceled.status);
        assertEquals(12, product1.stock);
        assertEquals(6, product2.stock);
        verify(products).save(product1);
        verify(products).save(product2);
        verify(orders).save(order);
    }

    @Test
    @DisplayName("Edge Case: Deve recompor o estoque mesmo se o produto estiver com status INACTIVE, mantendo INACTIVE")
    void shouldRestoreStockEvenIfProductIsInactive() {
        product1.status = ProductStatus.INACTIVE;
        when(orders.findById("order-abc")).thenReturn(Optional.of(order));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder canceled = orderService.cancel("order-abc", "user-123", false);

        assertNotNull(canceled);
        assertEquals(12, product1.stock, "Estoque deve ser recomposto normalmente");
        assertEquals(ProductStatus.INACTIVE, product1.status, "Produto deve permanecer INACTIVE");
        verify(products).save(product1);
    }

    @Test
    @DisplayName("Deve lançar 404 Not Found quando pedido não for encontrado")
    void shouldThrowNotFoundWhenOrderDoesNotExist() {
        when(orders.findById("inexistente")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                orderService.cancel("inexistente", "user-123", false)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.status);
    }
}
