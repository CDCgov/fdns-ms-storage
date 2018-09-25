package gov.cdc.foundation.security;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;

public class CustomUserAuthenticationConverter extends DefaultUserAuthenticationConverter {

	@SuppressWarnings("unused")
	private String USERNAME = "sub";

	@SuppressWarnings("unused")
	private Collection<? extends GrantedAuthority> defaultAuthorities;

	@SuppressWarnings("unused")
	private UserDetailsService userDetailsService;

	/**
	 * Optional {@link UserDetailsService} to use when extracting an
	 * {@link Authentication} from the incoming map.
	 * 
	 * @param userDetailsService
	 *            the userDetailsService to set
	 */
	public void setUserDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}
	
}
