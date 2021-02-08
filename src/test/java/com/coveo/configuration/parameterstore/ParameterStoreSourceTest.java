package com.coveo.configuration.parameterstore;

import com.coveo.configuration.parameterstore.exception.ParameterStoreError;
import com.coveo.configuration.parameterstore.exception.ParameterStoreParameterNotFoundError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParameterStoreSourceTest {
    private static final String VALID_PROPERTY_NAME = "awesomeproperty";
    private static final String VALID_PROPERTY_VALUE = "awesomepropertyVALUE";

    private static final String INVALID_PROPERTY_NAME = "notawesomeproperty";

    @Mock
    private SsmClient ssmClientMock;
    @Mock
    private SdkHttpResponse sdkHttpMetadataMock;
    @Mock
    private AwsResponseMetadata responseMetadataMock;

    private ParameterStoreSource parameterStoreSource;

    @Before
    public void setUp() {
        when(sdkHttpMetadataMock.statusCode()).thenReturn(200);

        parameterStoreSource = new ParameterStoreSource(ssmClientMock, false);
    }

    @Test
    public void testGetProperty() {
        GetParameterResponse.Builder getParameterResult = getGetParameterResult();
        when(
                ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME)))
                .thenReturn(getParameterResult
                        .parameter
                                (
                                        Parameter.builder().value(VALID_PROPERTY_VALUE).build()).build()
                );

        Object value = parameterStoreSource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value, is(VALID_PROPERTY_VALUE));
    }

    @Test
    public void testGetPropertyWhenNotFoundReturnsNull() {
        when(ssmClientMock.getParameter(getParameterRequest(INVALID_PROPERTY_NAME))).thenThrow(ParameterNotFoundException.builder().build());

        Object value = parameterStoreSource.getProperty(INVALID_PROPERTY_NAME);

        assertThat(value, is(nullValue()));
    }

    @Test(expected = ParameterStoreError.class)
    public void shouldThrowOnUnexpectedExceptionAccessingParameterStore() {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenThrow(new RuntimeException());

        parameterStoreSource.getProperty(VALID_PROPERTY_NAME);
    }

    @Test(expected = ParameterStoreParameterNotFoundError.class)
    public void shouldThrowOnGetPropertyWhenNotFoundAndHaltBootIsTrue() {
        when(ssmClientMock.getParameter(getParameterRequest(INVALID_PROPERTY_NAME))).thenThrow(ParameterNotFoundException.builder().build());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);

        parameterStoreSourceHaltingBoot.getProperty(INVALID_PROPERTY_NAME);
    }

    @Test(expected = ParameterStoreError.class)
    public void shouldThrowWhenStatusCodeIsNot200() {
        when(sdkHttpMetadataMock.statusCode()).thenReturn(503);
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(getGetParameterResult().build());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);

        parameterStoreSourceHaltingBoot.getProperty(VALID_PROPERTY_NAME);
    }

    @Test(expected = ParameterStoreError.class)
    public void shouldThrowWhenParameterIsNull() {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(getGetParameterResult().build());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);

        parameterStoreSourceHaltingBoot.getProperty(VALID_PROPERTY_NAME);
    }

    @Test(expected = ParameterStoreError.class)
    public void shouldThrowWhenParameterValueIsNull() {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(getGetParameterResult()
                .parameter(Parameter.builder().value(null).build()
                ).build());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);

        parameterStoreSourceHaltingBoot.getProperty(VALID_PROPERTY_NAME);
    }

    private GetParameterResponse.Builder getGetParameterResult() {
        GetParameterResponse.Builder builder = GetParameterResponse.builder();
        AwsResponseMetadata fakeAwsResponseMetadata = new AwsResponseMetadata(new HashMap<>()) {
            @Override
            public String requestId() {
                return super.requestId();
            }
        };
        builder.responseMetadata(fakeAwsResponseMetadata);
        builder.sdkHttpResponse(sdkHttpMetadataMock);
        return builder;

    }

    private GetParameterRequest getParameterRequest(String parameterName) {
        return GetParameterRequest.builder().name(parameterName).withDecryption(true).build();
    }
}
