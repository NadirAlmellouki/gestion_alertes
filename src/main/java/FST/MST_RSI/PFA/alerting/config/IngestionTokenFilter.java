package FST.MST_RSI.PFA.alerting.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@EnableConfigurationProperties(DynatraceIngestionProperties.class)
public class IngestionTokenFilter extends OncePerRequestFilter {

    private static final String INGESTION_PATH_PREFIX = "/api/v1/ingestion/";
    private static final String TOKEN_HEADER = "X-Ingestion-Token";

    private final DynatraceIngestionProperties properties;

    public IngestionTokenFilter(DynatraceIngestionProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(INGESTION_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || !token.equals(properties.token())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid ingestion token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
