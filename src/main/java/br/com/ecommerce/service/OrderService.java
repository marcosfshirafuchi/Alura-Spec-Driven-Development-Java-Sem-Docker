package br.com.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.*;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {
    private final PurchaseOrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final CartService carts;

    public OrderService(PurchaseOrderRepository o, ProductRepository p, UserRepository u, CartService c) {
        orders = o;
        products = p;
        users = u;
        carts = c;
    }

    public List<PurchaseOrder> list(String user) {
        return orders.findByUserId(user);
    }

    public PurchaseOrder find(String id) {
        return orders.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
    }

    @Transactional
    public PurchaseOrder checkout(String userId) {
        List<CartService.CartItem> cart = carts.get(userId).items();
        if (cart.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Carrinho vazio");
        PurchaseOrder order = new PurchaseOrder();
        order.user = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        for (var item : cart) {
            Product p = products.findById(item.productId()).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Produto não existe mais"));
            if (p.status != ProductStatus.ACTIVE || p.stock < item.quantity())
                throw new ApiException(HttpStatus.CONFLICT, "Produto indisponível ou estoque insuficiente: " + p.name);
            p.stock -= item.quantity();
            products.save(p);
            OrderItem oi = new OrderItem();
            oi.order = order;
            oi.product = p;
            oi.quantity = item.quantity();
            oi.price = p.price;
            order.items.add(oi);
            order.totalValue = order.totalValue.add(p.price.multiply(BigDecimal.valueOf(item.quantity())));
        }
        PurchaseOrder saved = orders.saveAndFlush(order);
        carts.clear(userId);
        return saved;
    }

    @Transactional
    public PurchaseOrder cancel(String id, String userId, boolean isAdmin) {
        PurchaseOrder order = find(id);
        if (!isAdmin && !order.user.id.equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
        order.items.size();
        if (order.status == OrderStatus.CANCELED) {
            return order;
        }
        if (order.status == OrderStatus.SHIPPED || order.status == OrderStatus.DELIVERED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Pedidos já enviados ou entregues não podem ser cancelados");
        }
        order.status = OrderStatus.CANCELED;
        for (OrderItem item : order.items) {
            Product p = item.product;
            p.stock += item.quantity;
            products.save(p);
        }
        return orders.save(order);
    }

    public PurchaseOrder cancel(String id, String userId) {
        return cancel(id, userId, false);
    }
}
