package com.coveo.configuration.parameterstore;

import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty.ACCEPTED_PROFILE;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty.ACCEPTED_PROFILES;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty.ENABLED;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty.SUPPORT_MULTIPLE_APPLICATION_CONTEXTS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl;

@RunWith(MockitoJUnitRunner.class)
public class ParameterStorePropertySourceEnvironmentPostProcessorTest
{
    private static final String[] EMPTY_CUSTOM_PROFILES = new String[] {};
    private static final String[] CUSTOM_PROFILES = new String[] { "open", "source", "this" };

    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private SpringApplication applicationMock;
    @Mock
    private ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory postProcessStrategyFactoryMock;
    @Mock
    private ParameterStorePropertySourceEnvironmentPostProcessStrategy defaultPostProcessStrategyMock;
    @Mock
    private ParameterStorePropertySourceEnvironmentPostProcessStrategy multiRegionPostProcessStrategyMock;

    private ParameterStorePropertySourceEnvironmentPostProcessor parameterStorePropertySourceEnvironmentPostProcessor = new ParameterStorePropertySourceEnvironmentPostProcessor();

    @Before
    public void setUp()
    {
        ParameterStorePropertySourceEnvironmentPostProcessor.initialized = false;

        when(postProcessStrategyFactoryMock.getStrategy(ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.DEFAULT_STRATEGY)).thenReturn(defaultPostProcessStrategyMock);
        when(postProcessStrategyFactoryMock.getStrategy(ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.MULTI_REGION_STRATEGY)).thenReturn(multiRegionPostProcessStrategyMock);
        ParameterStorePropertySourceEnvironmentPostProcessor.postProcessStrategyFactory = postProcessStrategyFactoryMock;

        when(configurableEnvironmentMock.getProperty(ENABLED, Boolean.class, Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getProperty(SUPPORT_MULTIPLE_APPLICATION_CONTEXTS,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
    }

    @Test
    public void testParameterStoreIsDisabledByDefault()
    {
        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyZeroInteractions(applicationMock);
        verifyZeroInteractions(defaultPostProcessStrategyMock);
        verifyZeroInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithPropertySetToTrue()
    {
        when(configurableEnvironmentMock.getProperty(ENABLED, Boolean.class, Boolean.FALSE)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).postProcess(configurableEnvironmentMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithProfile()
    {
        when(configurableEnvironmentMock.acceptsProfiles(ACCEPTED_PROFILE)).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).postProcess(configurableEnvironmentMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithCustomProfiles()
    {
        when(configurableEnvironmentMock.getProperty(ACCEPTED_PROFILES, String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES)).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).postProcess(configurableEnvironmentMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesEmpty()
    {
        when(configurableEnvironmentMock.getProperty(ACCEPTED_PROFILES,
                                                     String[].class)).thenReturn(EMPTY_CUSTOM_PROFILES);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(configurableEnvironmentMock, never()).acceptsProfiles(EMPTY_CUSTOM_PROFILES);
        verifyZeroInteractions(defaultPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesButNoneOfTheProfilesActive()
    {
        when(configurableEnvironmentMock.getProperty(ACCEPTED_PROFILES, String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES)).thenReturn(false);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyZeroInteractions(defaultPostProcessStrategyMock);
    }

    @Test
    public void testParameterStorePropertySourceEnvironmentPostProcessorCantBeCalledTwice()
    {
        when(configurableEnvironmentMock.getProperty(ENABLED, Boolean.class, Boolean.FALSE)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).postProcess(configurableEnvironmentMock);
    }

    @Test
    public void testParameterStorePropertySourceEnvironmentPostProcessorCanBeCalledTwiceWhenDisablingMultipleContextSupport()
    {
        when(configurableEnvironmentMock.getProperty(ENABLED, Boolean.class, Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(SUPPORT_MULTIPLE_APPLICATION_CONTEXTS,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock, times(2)).postProcess(configurableEnvironmentMock);
    }

    @Test
    public void testWhenMultiRegionIsEnabled()
    {
        when(configurableEnvironmentMock.getProperty(ENABLED, Boolean.class, Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.containsProperty(ParameterStorePropertySourceConfigurationProperty.SSM_CLIENT_SIGNING_REGIONS)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(multiRegionPostProcessStrategyMock, times(1)).postProcess(configurableEnvironmentMock);
    }
}
