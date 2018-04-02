package filters;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

@WebFilter(urlPatterns = "/*")
public class ErrorFilter implements Filter {
    private static Logger log = Logger.getLogger(ErrorFilter.class);


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        try{
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Throwable e) {
            log.error(e.getMessage());
            // refer to ERROR_PAGE
            String ERROR_PAGE = "/WEB-INF/view/layouts/anyError.jspx";
            servletRequest.getRequestDispatcher(ERROR_PAGE).forward(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {

    }
}
