package br.com.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.*;

import java.util.*;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final OrderService orders;
    private final PurchaseOrderRepository orderRepo;

    public PaymentService(PaymentRepository p, OrderService o, PurchaseOrderRepository r) {
        payments = p;
        orders = o;
        orderRepo = r;
    }

    public Payment create(String orderId, String userId) {
        PurchaseOrder o = orders.find(orderId);
        if (!o.user.id.equals(userId)) throw new ApiException(HttpStatus.FORBIDDEN, "Acesso negado");
        return payments.findByOrderId(orderId).orElseGet(() -> {
            Payment p = new Payment();
            p.order = o;
            return payments.save(p);
        });
    }

    @Transactional
    public Payment confirm(String id, String userId, String key) {
        if (key == null || key.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key obrigatório");
        Payment p = payment(id, userId);
        if (p.status.equals("PAID")) return p;
        if (p.status.equals("CANCELED")) throw new ApiException(HttpStatus.CONFLICT, "Pagamento cancelado");
        p.status = "PAID";
        p.order.status = OrderStatus.PAID;
        orderRepo.save(p.order);
        return payments.save(p);
    }

    @Transactional
    public Object cancel(String id, String userId) {
        Payment p = payment(id, userId);
        if (p.status.equals("PAID")) throw new ApiException(HttpStatus.CONFLICT, "Pagamento já confirmado");
        if (!p.status.equals("CANCELED")) {
            p.status = "CANCELED";
            p.order.status = OrderStatus.CANCELED;
            for (OrderItem item : p.order.items) {
                item.product.stock += item.quantity;
            }
            orderRepo.save(p.order);
            payments.save(p);
        }
        return Map.of("message", "Pagamento e pedido cancelados");
    }

    private Payment payment(String id, String userId) {
        Payment p = payments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"));
        if (!p.order.user.id.equals(userId)) throw new ApiException(HttpStatus.FORBIDDEN, "Acesso negado");
        return p;
    }
}
