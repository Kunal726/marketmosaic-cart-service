package com.projects.marketmosaic.config;

import com.projects.marketmosaic.common.dto.resp.TokenValidationRespDTO;
import com.projects.marketmosaic.common.exception.MarketMosaicCommonException;
import com.projects.marketmosaic.common.utils.UserUtils;
import com.projects.marketmosaic.constants.Constants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

	private final ObjectProvider<UserUtils> userUtilsObjectProvider;

	public AuthInterceptor(ObjectProvider<UserUtils> userUtilsObjectProvider) {
		this.userUtilsObjectProvider = userUtilsObjectProvider;
	}

	private static final Set<String> EXCLUDED_PATHS = Set.of("/health", "/public/info");

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String path = request.getRequestURI();

		if (EXCLUDED_PATHS.contains(path)) {
			return true; // Skip auth for excluded paths
		}

		UserUtils userUtils = userUtilsObjectProvider.getObject();

		try {
			userUtils.validateUser(request);
			return true;
		} catch (MarketMosaicCommonException ex) {
			log.info("Session Not Found guest user found");
			if(ex.getCode() == 40000) {
				request.setAttribute("userId", Constants.GUEST_USER_ID);
				return true;
			}
			return false;
		}
	}
}
