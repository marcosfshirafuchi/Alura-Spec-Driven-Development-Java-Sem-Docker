package br.com.ecommerce.controller;

import br.com.ecommerce.config.ErrorHandler;
import br.com.ecommerce.config.SecurityConfig;
import br.com.ecommerce.model.OrderStatus;
import br.com.ecommerce.model.PurchaseOrder;
import br.com.ecommerce.model.User;
import br.com.ecommerce.service.ApiException;
import br.com.ecommerce.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, ErrorHandler.class})
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("DELETE /order/{id} - Retorna 200 quando cancelado com sucesso pelo titular")
    void cancelOrderAsOwnerReturns200() throws Exception {
        PurchaseOrder mockOrder = new PurchaseOrder();
        mockOrder.id = "ord-100";
        mockOrder.status = OrderStatus.CANCELED;

        User owner = new User();
        owner.id = "user-1";
        mockOrder.user = owner;

        when(orderService.cancel(eq("ord-100"), eq("user-1"), eq(false))).thenReturn(mockOrder);

        mockMvc.perform(delete("/order/ord-100")
                        .with(jwt().jwt(j -> j.subject("user-1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ord-100"))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    @Test
    @DisplayName("DELETE /order/{id} - Retorna 200 quando cancelado por um ADMIN")
    void cancelOrderAsAdminReturns200() throws Exception {
        PurchaseOrder mockOrder = new PurchaseOrder();
        mockOrder.id = "ord-100";
        mockOrder.status = OrderStatus.CANCELED;

        when(orderService.cancel(eq("ord-100"), eq("admin-99"), eq(true))).thenReturn(mockOrder);

        mockMvc.perform(delete("/order/ord-100")
                        .with(jwt().jwt(j -> j.subject("admin-99").claim("role", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    @Test
    @DisplayName("DELETE /order/{id} - Retorna 403 Forbidden quando usuário não é titular nem admin")
    void cancelOrderNonOwnerReturns403() throws Exception {
        when(orderService.cancel(eq("ord-100"), eq("user-intruder"), eq(false)))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(delete("/order/ord-100")
                        .with(jwt().jwt(j -> j.subject("user-intruder").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Acesso negado"));
    }

    @Test
    @DisplayName("DELETE /order/{id} - Retorna 400 Bad Request quando pedido já foi despachado (SHIPPED)")
    void cancelShippedOrderReturns400() throws Exception {
        when(orderService.cancel(eq("ord-100"), eq("user-1"), eq(false)))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Pedidos já enviados ou entregues não podem ser cancelados"));

        mockMvc.perform(delete("/order/ord-100")
                        .with(jwt().jwt(j -> j.subject("user-1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("Pedidos já enviados ou entregues não podem ser cancelados"));
    }

    @Test
    @DisplayName("DELETE /order/{id} - Retorna 404 Not Found quando pedido não existe")
    void cancelNotFoundOrderReturns404() throws Exception {
        when(orderService.cancel(eq("ord-999"), eq("user-1"), eq(false)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        mockMvc.perform(delete("/order/ord-999")
                        .with(jwt().jwt(j -> j.subject("user-1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message").value("Pedido não encontrado"));
    }

    @Test
    @DisplayName("DELETE /order/{id} - Retorna 401 Unauthorized sem token JWT")
    void cancelWithoutTokenReturns401() throws Exception {
        mockMvc.perform(delete("/order/ord-100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /order/{id}/cancel - Endpoint anterior desativado/inexistente deve retornar 404 Not Found")
    void oldPostCancelEndpointMustNotExist() throws Exception {
        mockMvc.perform(post("/order/ord-100/cancel")
                        .with(jwt().jwt(j -> j.subject("user-1").claim("role", "USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /order/{id}/items/{itemId} - Endpoint inexistente garante proibição de cancelamento parcial")
    void deleteOrderItemEndpointMustNotExist() throws Exception {
        mockMvc.perform(delete("/order/ord-100/items/item-1")
                        .with(jwt().jwt(j -> j.subject("user-1").claim("role", "USER"))))
                .andExpect(status().isNotFound());
    }
}
