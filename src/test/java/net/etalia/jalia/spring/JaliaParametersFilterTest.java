package net.etalia.jalia.spring;

import net.etalia.jalia.OutField;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.StringReader;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JaliaParametersFilterTest {

    @Mock
    public ServletRequest request;

    @Mock
    public ServletResponse response;

    @Mock
    public FilterChain filterChain;

    @Mock
    public FilterConfig filterConfig;

    private JaliaParametersFilter filter;

    @Before
    public void setup() {
        filter = new JaliaParametersFilter();
    }

    @Test
    public void shouldSetFields() throws Exception {
        doReturn("fields").when(filterConfig).getInitParameter(eq("parameterName"));
        filter.init(filterConfig);
        doReturn("field1,field2").when(request).getParameter("fields");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutField fields = JaliaParametersFilter.getFields();
                assertThat(fields, notNullValue());
                assertThat(fields.getSub("field1"), notNullValue());
                assertThat(fields.getSub("field2"), notNullValue());
                return null;
            }
        }).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(same(request), same(response));
        assertThat(JaliaParametersFilter.getFields(), nullValue());
    }

    @Test
    public void shouldIgnoreFields() throws Exception {
        filter.init(filterConfig);
        doReturn("field1,field2").when(request).getParameter("fields");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutField fields = JaliaParametersFilter.getFields();
                assertThat(fields, nullValue());
                return null;
            }
        }).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(same(request), same(response));
        assertThat(JaliaParametersFilter.getFields(), nullValue());
    }

    @Test
    public void shouldSetGroup() throws Exception {
        doReturn("group").when(filterConfig).getInitParameter(eq("groupParameterName"));
        filter.init(filterConfig);
        doReturn("groupName").when(request).getParameter("group");
        OutField.getGroups().clear();
        OutField.parseGroupsJson(new StringReader("{'groupName': 'field1'}".replace("'","\"")));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutField fields = JaliaParametersFilter.getFields();
                assertThat(fields, notNullValue());
                assertThat(fields.getSub("field1"), notNullValue());
                return null;
            }
        }).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(same(request), same(response));
        assertThat(JaliaParametersFilter.getFields(), nullValue());
    }

    @Test
    public void shouldIgnoreGroup() throws Exception {
        filter.init(filterConfig);
        doReturn("groupName").when(request).getParameter("group");
        OutField.getGroups().clear();
        OutField.parseGroupsJson(new StringReader("{'groupName': 'field1'}".replace("'","\"")));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutField fields = JaliaParametersFilter.getFields();
                assertThat(fields, nullValue());
                return null;
            }
        }).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(same(request), same(response));
        assertThat(JaliaParametersFilter.getFields(), nullValue());
    }

    @Test
    public void shouldIgnoreUnknownGroup() throws Exception {
        doReturn("group").when(filterConfig).getInitParameter(eq("groupParameterName"));
        filter.init(filterConfig);
        doReturn("groupName").when(request).getParameter("group");
        OutField.getGroups().clear();
        OutField.parseGroupsJson(new StringReader("{'otherName': 'field1'}".replace("'","\"")));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                OutField fields = JaliaParametersFilter.getFields();
                assertThat(fields, nullValue());
                return null;
            }
        }).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(same(request), same(response));
        assertThat(JaliaParametersFilter.getFields(), nullValue());
    }
}