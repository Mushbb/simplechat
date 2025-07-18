//package com.example.simplechat.controller;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.web.servlet.HandlerInterceptor; // 인터셉터 인터페이스
//import org.springframework.web.servlet.ModelAndView; // 필요에 따라 사용 (여기서는 사용하지 않음)
//
//public class AuthInterceptor implements HandlerInterceptor {
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        String requestURI = request.getRequestURI(); 
//        System.out.println("DEBUG: [AuthInterceptor] - RequestURI: " + requestURI);
//
//        // WebMvcConfig에서 이미 /test/login만 필터링하도록 설정되었으므로
//        // 여기서는 해당 요청이 로그인 중복 시도인지 아닌지만 확인합니다.
//
//        HttpSession session = request.getSession(false); // 세션이 없으면 새로 생성하지 않음
//
//        // 세션이 있고, 이미 로그인 정보가 있는 경우
//        if (session != null && session.getAttribute("loggedInUser") != null) {
//            String loggedInUser = (String) session.getAttribute("loggedInUser");
//            System.out.println("DEBUG: [AuthInterceptor] - User '" + loggedInUser + "' is already logged in. Denying duplicate login request.");
//            
//            // 로그인 정보를 클라이언트에게 돌려주러 감
//            return true; // 컨트롤러의 /test/login 로직으로 넘어가서 중복 로그인을 처리하게 함.
//        }
//
//        // 로그인되어 있지 않은 경우, /test/login 요청은 정상적으로 처리하도록 허용합니다.
//        System.out.println("DEBUG: [AuthInterceptor] - User not logged in. Allowing login attempt.");
//        // 로그인이 안되어 있으니, 로그인 페이지로 입장
//        response.sendRedirect("/test/redirect");
//        return false;
//    }
//
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        // 이 경우 /test/login 처리 후 필요하면 로직 추가
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        // 이 경우 /test/login 처리 완료 후 필요하면 로직 추가
//    }
//}