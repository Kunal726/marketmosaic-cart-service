package com.projects.marketmosaic.dto.resp;

import com.projects.marketmosaic.common.dto.resp.BaseRespDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CartDTO extends BaseRespDTO {
    private String id;
    private List<CartItemDTO> items;
    private BigDecimal totalAmount;
}
