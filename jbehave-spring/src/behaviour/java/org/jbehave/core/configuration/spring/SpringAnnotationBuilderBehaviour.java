package org.jbehave.core.configuration.spring;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jbehave.core.reporters.StoryReporterBuilder.Format.CONSOLE;
import static org.jbehave.core.reporters.StoryReporterBuilder.Format.HTML;
import static org.jbehave.core.reporters.StoryReporterBuilder.Format.STATS;
import static org.jbehave.core.reporters.StoryReporterBuilder.Format.TXT;
import static org.jbehave.core.reporters.StoryReporterBuilder.Format.XML;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jbehave.core.annotations.Configure;
import org.jbehave.core.annotations.spring.UsingSpring;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.failures.SilentlyAbsorbingFailure;
import org.jbehave.core.io.LoadFromURL;
import org.jbehave.core.parsers.RegexPrefixCapturingPatternParser;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.ParameterConverters;
import org.jbehave.core.steps.Steps;
import org.jbehave.core.steps.spring.SpringStepsFactoryBehaviour.FooSteps;
import org.jbehave.core.steps.spring.SpringStepsFactoryBehaviour.FooStepsWithDependency;
import org.junit.Assert;
import org.junit.Test;

public class SpringAnnotationBuilderBehaviour {

    private SpringAnnotationBuilder builder = new SpringAnnotationBuilder();

    @Test
    public void shouldBuildConfigurationFromAnnotations() {
        Configuration configuration = builder.buildConfiguration(new Annotated());
        assertThat(configuration.failureStrategy(), instanceOf(SilentlyAbsorbingFailure.class));
        assertThat(configuration.storyLoader(), instanceOf(LoadFromURL.class));
        assertThat(configuration.stepPatternParser(), instanceOf(RegexPrefixCapturingPatternParser.class));
        assertThat(configuration.stepPatternParser().toString(), containsString("prefix=MyPrefix"));
        assertThatDateIsConvertedWithFormat(configuration.parameterConverters(), new SimpleDateFormat("yyyy-MM-dd"));
        assertThat(configuration.storyReporterBuilder().formats(), hasItems(CONSOLE, HTML, TXT, XML, STATS));
        assertThat(configuration.storyReporterBuilder().outputDirectory().getName(), equalTo("my-output-directory"));
        assertThat(configuration.storyReporterBuilder().viewResources().getProperty("index"), equalTo("my-reports-index.ftl"));
        assertThat(configuration.storyReporterBuilder().viewResources().getProperty("decorateNonHtml"), equalTo("true"));
        assertThat(configuration.storyReporterBuilder().reportFailureTrace(), is(true));        
    }

    private void assertThatDateIsConvertedWithFormat(ParameterConverters parameterConverters, DateFormat dateFormat) {
        String date = "2010-10-10";
        try {
            assertThat((Date) parameterConverters.convert(date, Date.class), equalTo(dateFormat.parse(date)));
        } catch (ParseException e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldBuildDefaultConfigurationIfAnnotationOrAnnotatedValuesNotPresent() {
        assertThatConfigurationIs(builder.buildConfiguration(new NotAnnotated()), new MostUsefulConfiguration());
        assertThatConfigurationIs(builder.buildConfiguration(new AnnotatedWithoutLocations()), new MostUsefulConfiguration());
    }

    private void assertThatConfigurationIs(Configuration builtConfiguration,
            Configuration defaultConfiguration) {
        assertThat(builtConfiguration.failureStrategy(), instanceOf(defaultConfiguration.failureStrategy().getClass()));
        assertThat(builtConfiguration.storyLoader(), instanceOf(defaultConfiguration.storyLoader().getClass()));
        assertThat(builtConfiguration.stepPatternParser(), instanceOf(defaultConfiguration.stepPatternParser().getClass()));
        assertThat(builtConfiguration.storyReporterBuilder().formats(), equalTo(defaultConfiguration.storyReporterBuilder().formats()));
        assertThat(builtConfiguration.storyReporterBuilder().outputDirectory(), equalTo(defaultConfiguration.storyReporterBuilder().outputDirectory()));
        assertThat(builtConfiguration.storyReporterBuilder().viewResources(), equalTo(defaultConfiguration.storyReporterBuilder().viewResources()));
        assertThat(builtConfiguration.storyReporterBuilder().reportFailureTrace(), equalTo(defaultConfiguration.storyReporterBuilder().reportFailureTrace()));
    }

    @Test
    public void shouldBuildCandidateStepsFromAnnotations() {
        assertThatStepsInstancesAre(builder.buildCandidateSteps(new Annotated()), FooSteps.class,
                FooStepsWithDependency.class);
    }

    @Test
    public void shouldBuildEmptyStepsListIfAnnotationOrAnnotatedValuesNotPresent() {
        assertThatStepsInstancesAre(builder.buildCandidateSteps(new NotAnnotated()));
        assertThatStepsInstancesAre(builder.buildCandidateSteps(new AnnotatedWithoutLocations()));
    }

    private void assertThatStepsInstancesAre(List<CandidateSteps> candidateSteps, Class<?>... stepsClasses) {
        assertThat(candidateSteps.size(), equalTo(stepsClasses.length));
        for (int i = 0; i < stepsClasses.length; i++) {
            assertThat(((Steps) candidateSteps.get(i)).instance(), instanceOf(stepsClasses[i]));
        }
    }

    @Configure()
    @UsingSpring(locations = { "org/jbehave/core/configuration/spring/configuration.xml",
            "org/jbehave/core/steps/spring/steps.xml", "org/jbehave/core/steps/spring/steps-with-dependency.xml" })
    private static class Annotated {

    }

    @Configure()
    @UsingSpring()
    private static class AnnotatedWithoutLocations {

    }

    private static class NotAnnotated {

    }

}
