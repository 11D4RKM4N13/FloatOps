package com.floatops.dto.part;

import java.math.BigDecimal;
import java.util.UUID;

public class PartCreateDTO {

    public String code;
    public String name; 
    public BigDecimal price;
    public int stock;
    public UUID categoryId;
}