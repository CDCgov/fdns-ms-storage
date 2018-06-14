package gov.cdc.foundation.security;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.util.StringUtils;

public class CustomUserAuthenticationConverter extends DefaultUserAuthenticationConverter {

	private String USERNAME = "sub";

	private Collection<? extends GrantedAuthority> defaultAuthorities;

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
