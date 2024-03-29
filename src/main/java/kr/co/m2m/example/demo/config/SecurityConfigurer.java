package kr.co.m2m.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import kr.co.m2m.example.demo.common.handler.BEFailureHandler;
import kr.co.m2m.example.demo.common.handler.BESuccessHandler;
import kr.co.m2m.example.framework.auth.BEAuthenticationFilter;
import kr.co.m2m.example.framework.auth.BEAuthenticationProvider;
import kr.co.m2m.example.framework.auth.RedisBEAuthManager;
import kr.co.m2m.example.framework.auth.handler.BEAccessDeniedHandler;
import kr.co.m2m.example.framework.auth.handler.BEAuthenticationEntryPoint;

/**
 * @패키지명 : kr.co.m2m.example.demo.config
 * @파일명 : SecurityConfigurer.java
 * @작성자 : wtkim
 * @생성일자 : 2020. 6. 5.
 * @설명 : Spring Security 설정 Configurer
 */

@Configuration
@EnableWebSecurity
public class SecurityConfigurer extends WebSecurityConfigurerAdapter {

	/**
	 * 로그인 정보 인증 Provider
	 */
	@Autowired
	private BEAuthenticationProvider beAuthProvider;

	/**
	 * REDIS 인증 manager
	 */
	@Autowired
	private RedisBEAuthManager redisBEAuthManager;

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(this.beAuthProvider);
	}

	/**
	 * Spring Security 처리예외 설정
	 */
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/css/**", "/js/**", "/img/**", "/lib/**", // WEB관련 static 자료
				"/v2/api-docs", "/swagger-resources/**", "/swagger-ui.html", "/webjars/**", "/swagger/**" // Swagger UI관련
		);
	}

	/**
	 * 이하 Spring Security 관련설정 - csrf, remember-me, cors, filter, 인증 URI패턴 handler
	 * 로그인처리 등
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// BackEnd 인증용 필터, 추후 @Autowired로 변환
		BEAuthenticationFilter beAuthFilter = new BEAuthenticationFilter(this.authenticationManager(), null);
		// csrf 끄기
		http.csrf().disable();
		// 인증 세션저장 끄기
		http.rememberMe().disable();
		// cors 설정
		http.cors().disable();
		// BackEnd 인증용 필터 등록, Tocken으로 인증하므로 별도 세션은 생성안함
		http.addFilterBefore(beAuthFilter, UsernamePasswordAuthenticationFilter.class).sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 세션생성 안함
		// 인증없이 접속가능한 패턴등록
		http.authorizeRequests().antMatchers("/", "/main", "/main/**", "/test", "/test/**", "/error", "/error/**", "/example*", "/example*/**").permitAll();
		http.authorizeRequests().antMatchers("/", "/main", "/main/**", "/test", "/test/**", "/error", "/error/**").permitAll()
				.antMatchers("/login", "/login/**").permitAll() // 로그인 인증필요 없음
				.antMatchers("/**").authenticated(); // 그외는 인증을 거치도록 함
		// 시큐리티 관련 Custom Handler등 등록
		http.exceptionHandling().accessDeniedHandler(new BEAccessDeniedHandler()).and().exceptionHandling()
				.authenticationEntryPoint(new BEAuthenticationEntryPoint()); // 401오류 제어
		// 로그인정보등록 및 SuccessHandler 등록
		http.formLogin().loginPage("/").loginPage("/login").usernameParameter("id").passwordParameter("password").loginProcessingUrl("/login_processing")
				.failureUrl("/login-error").successHandler(new BESuccessHandler(redisBEAuthManager)).failureHandler(new BEFailureHandler());
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// return PasswordEncoderFactories.createDelegatingPasswordEncoder();
		return new BCryptPasswordEncoder();
	}
}