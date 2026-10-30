package com.bank.api.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import javax.servlet.FilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.Test;

public class CorrelationIdFilterTest {
    @Test public void echoesValidCallerCorrelationId() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter(); MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader(CorrelationIdFilter.HEADER, "qa-run-1001"); MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(response.getHeader(CorrelationIdFilter.HEADER), "qa-run-1001"); verify(chain).doFilter(request, response);
    }
    @Test public void generatesCorrelationIdWhenCallerDoesNotSupplyOne() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter(); MockHttpServletResponse response = new MockHttpServletResponse(); filter.doFilter(new MockHttpServletRequest(), response, mock(FilterChain.class)); assertNotNull(response.getHeader(CorrelationIdFilter.HEADER));
    }
}
