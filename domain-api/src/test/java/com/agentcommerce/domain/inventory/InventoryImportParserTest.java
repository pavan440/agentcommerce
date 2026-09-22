package com.agentcommerce.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class InventoryImportParserTest {
 private final InventoryImportParser parser=new InventoryImportParser();
 @Test void parsesValidAndInvalidRows(){var rows=parser.parse("sku,name,price,quantity_on_hand,is_available,category,reorder_threshold,image_url\nA-1,Daily Tiffin,12.50,20,true,Meals,5,https://cdn.example.com/tiffin.jpg\nA-2,Bad Price,abc,2,true,Meals,0,");assertThat(rows).hasSize(2);assertThat(rows.get(0).valid()).isTrue();assertThat(rows.get(0).priceMinor()).isEqualTo(1250);assertThat(rows.get(0).imageUrl()).isEqualTo("https://cdn.example.com/tiffin.jpg");assertThat(rows.get(1).valid()).isFalse();assertThat(rows.get(1).error()).contains("price");}
 @Test void supportsQuotedCommas(){var row=parser.parse("sku,name,price,quantity_on_hand,is_available\nA-1,\"Rice, Curry and Dal\",9.99,4,true").getFirst();assertThat(row.name()).isEqualTo("Rice, Curry and Dal");}
 @Test void rejectsMissingHeaders(){assertThatThrownBy(()->parser.parse("sku,name\nA,Item")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("requires");}
}
