/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.core.order.service.call;

import java.util.HashMap;
import java.util.Map;

/**
 * Only the product and quantity are required to add an item to an order.
 *
 * The category can be inferred from the product's default category.
 *
 * The sku can be inferred from either the passed in attributes as they are compared to the product's options or
 * the sku can be determined from the product's default sku.
 *
 */
public class OrderItemRequestDTO {

    private Long skuId;
    private Long categoryId;
    private Long productId;
    private Long orderItemId;
    private Integer quantity;
    private Map<String,String> itemAttributes = new HashMap<String,String>();
    
    public OrderItemRequestDTO() {}
    
    public OrderItemRequestDTO(Long productId, Integer quantity) {
    	setProductId(productId);
    	setQuantity(quantity);
    }
    
    public OrderItemRequestDTO(Long productId, Long skuId, Integer quantity) {
    	setProductId(productId);
    	setSkuId(skuId);
    	setQuantity(quantity);
    }
    
    public OrderItemRequestDTO(Long productId, Long skuId, Long categoryId, Integer quantity) {
    	setProductId(productId);
    	setSkuId(skuId);
    	setCategoryId(categoryId);
    	setQuantity(quantity);
    }

    public Long getSkuId() {
        return skuId;
    }

    public OrderItemRequestDTO setSkuId(Long skuId) {
        this.skuId = skuId;
        return this;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public OrderItemRequestDTO setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public Long getProductId() {
        return productId;
    }

    public OrderItemRequestDTO setProductId(Long productId) {
        this.productId = productId;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderItemRequestDTO setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Map<String, String> getItemAttributes() {
        return itemAttributes;
    }

    public OrderItemRequestDTO setItemAttributes(Map<String, String> itemAttributes) {
        this.itemAttributes = itemAttributes;
        return this;
    }
    
    public Long getOrderItemId() {
        return orderItemId;
    }

    public OrderItemRequestDTO setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
        return this;
    }

}
