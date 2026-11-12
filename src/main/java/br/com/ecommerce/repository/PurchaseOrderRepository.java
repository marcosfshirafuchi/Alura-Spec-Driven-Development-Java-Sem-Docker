package br.com.ecommerce.repository;

import br.com.ecommerce.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

import org.springframework.data.domain.*;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
    List<PurchaseOrder> findByUserId(String userId);
}
