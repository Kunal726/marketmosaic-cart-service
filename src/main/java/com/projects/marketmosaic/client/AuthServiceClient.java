package com.projects.marketmosaic.client;

import com.projects.marketmosaic.common.dto.resp.TokenValidationRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-gateway", url = "http://localhost:8081")
public interface AuthServiceClient {

	@GetMapping("/auth/validate-user")
	TokenValidationRespDTO validateUser(@RequestHeader("Cookie") String token);

}
