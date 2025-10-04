package com.projects.marketmosaic.dto;

import com.projects.marketmosaic.common.dto.resp.BaseRespDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CheckoutCartRespDto extends BaseRespDTO {
    private boolean redirectLogin;
    private boolean showPlaceOrder;
}
