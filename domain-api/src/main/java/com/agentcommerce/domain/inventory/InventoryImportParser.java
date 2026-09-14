package com.agentcommerce.domain.inventory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class InventoryImportParser {
    private static final List<String> REQUIRED = List.of("sku", "name", "price", "quantity_on_hand", "is_available");

    List<ParsedInventoryRow> parse(String content) {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("CSV file is empty");
            List<String> headers = split(headerLine).stream().map(v -> v.trim().toLowerCase(Locale.ROOT)).toList();
            if (!headers.containsAll(REQUIRED)) throw new IllegalArgumentException("CSV requires sku,name,price,quantity_on_hand,is_available");
            List<ParsedInventoryRow> rows = new ArrayList<>(); String line; int number = 1;
            while ((line = reader.readLine()) != null) { number++; if (line.isBlank()) continue; rows.add(parseRow(number, headers, split(line))); }
            if (rows.isEmpty()) throw new IllegalArgumentException("CSV contains no data rows");
            return rows;
        } catch (IOException exception) { throw new IllegalArgumentException("Unable to read CSV", exception); }
    }

    private ParsedInventoryRow parseRow(int number, List<String> headers, List<String> values) {
        Map<String,String> row = new HashMap<>(); for (int i=0;i<headers.size();i++) row.put(headers.get(i), i<values.size()?values.get(i).trim():"");
        List<String> errors = new ArrayList<>(); String sku=row.get("sku"), name=row.get("name"), category=blank(row.get("category"));
        if (sku.isBlank()) errors.add("sku is required"); if (name.isBlank()) errors.add("name is required");
        Long price = decimalMinor(row.get("price"), errors); Integer quantity = integer(row.get("quantity_on_hand"), "quantity_on_hand", errors);
        Boolean available = bool(row.get("is_available"), errors); Integer threshold = row.containsKey("reorder_threshold") ? integerDefault(row.get("reorder_threshold"), "reorder_threshold", errors, 0) : 0;
        String currency = row.getOrDefault("currency", "USD").toUpperCase(Locale.ROOT); if (!currency.matches("[A-Z]{3}")) errors.add("currency must be three letters");
        return new ParsedInventoryRow(number, sku, name, category, price, currency, quantity, available, threshold, String.join("; ", errors));
    }
    private Long decimalMinor(String value,List<String> errors){try{var d=new java.math.BigDecimal(value);if(d.signum()<0||d.scale()>2)throw new Exception();return d.movePointRight(2).longValueExact();}catch(Exception e){errors.add("price must be a non-negative decimal with at most 2 places");return null;}}
    private Integer integer(String value,String field,List<String> errors){return integerDefault(value,field,errors,null);} private Integer integerDefault(String value,String field,List<String> errors,Integer fallback){if(value==null||value.isBlank())return fallback;try{int n=Integer.parseInt(value);if(n<0)throw new Exception();return n;}catch(Exception e){errors.add(field+" must be a non-negative integer");return null;}}
    private Boolean bool(String value,List<String> errors){if("true".equalsIgnoreCase(value)||"1".equals(value))return true;if("false".equalsIgnoreCase(value)||"0".equals(value))return false;errors.add("is_available must be true or false");return null;}
    private String blank(String value){return value==null||value.isBlank()?null:value;}
    private List<String> split(String line){List<String> out=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quoted=false;for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){out.add(value.toString());value.setLength(0);}else value.append(ch);}out.add(value.toString());return out;}
}
record ParsedInventoryRow(int rowNumber,String sku,String name,String category,Long priceMinor,String currency,Integer quantityOnHand,Boolean available,Integer reorderThreshold,String error){boolean valid(){return error.isEmpty();}}