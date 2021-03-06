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

import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.order.domain.PersonalMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Only the product is required to add an item to an order.
 *
 * The category can be inferred from the product's default category.
 *
 * The sku can be inferred from either the passed in attributes as they are compared to the product's options or
 * the sku can be determined from the product's default sku.
 *
 * Personal message is optional.
 *
 */
public abstract class AbstractOrderItemRequest {

    private Sku sku;
    private Category category;
    private Product product;
    private int quantity;
    private PersonalMessage personalMessage;
    private Map<String,String> itemAttributes = new HashMap<String,String>();

    public Sku getSku() {
        return sku;
    }

    public void setSku(Sku sku) {
        this.sku = sku;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Map<String, String> getItemAttributes() {
        return itemAttributes;
    }

    public void setItemAttributes(Map<String, String> itemAttributes) {
        this.itemAttributes = itemAttributes;
    }

    protected void copyProperties(AbstractOrderItemRequest newRequest) {
        newRequest.setCategory(category);
        newRequest.setItemAttributes(itemAttributes);
        newRequest.setPersonalMessage(personalMessage);
        newRequest.setProduct(product);
        newRequest.setQuantity(quantity);
        newRequest.setSku(sku);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractOrderItemRequest)) return false;

        AbstractOrderItemRequest that = (AbstractOrderItemRequest) o;

        if (!category.equals(that.category)) return false;
        if (!product.equals(that.product)) return false;
        if (quantity != that.quantity) return false;
        if (!sku.equals(that.sku)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = product != null ? product.hashCode() : 0;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (sku != null ? sku.hashCode() : 0);
        result = 31 * result + quantity;
        return result;
    }

    public PersonalMessage getPersonalMessage() {
        return personalMessage;
    }

    public void setPersonalMessage(PersonalMessage personalMessage) {
        this.personalMessage = personalMessage;
    }
}
