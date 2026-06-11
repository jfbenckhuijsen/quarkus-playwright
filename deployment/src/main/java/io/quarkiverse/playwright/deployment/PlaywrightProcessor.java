package io.quarkiverse.playwright.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.impl.driver.jar.DriverJar;
import com.microsoft.playwright.options.HttpHeader;
import com.microsoft.playwright.options.Timing;
import com.microsoft.playwright.options.ViewportSize;

import io.quarkiverse.playwright.PlaywrightBuildTimeConfig;
import io.quarkiverse.playwright.PlaywrightRecorder;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.logging.Log;

class PlaywrightProcessor {

    private static final String FEATURE = "playwright";
    private static final String PLAYWRIGHT_ENDPOINT_CONFIG = "quarkus.playwright.endpoint";
    private static final int PLAYWRIGHT_SERVER_PORT = 3000;
    private static final String PLAYWRIGHT_SERVER_COMMAND = "npx -y playwright@1.60.0 run-server --host 0.0.0.0 --port "
            + PLAYWRIGHT_SERVER_PORT;

    private static volatile DevServicesResultBuildItem.RunningDevService runningDevService;
    private static volatile PlaywrightDevServiceConfiguration capturedDevServiceConfiguration;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexTransitiveDependencies(BuildProducer<IndexDependencyBuildItem> index) {
        index.produce(new IndexDependencyBuildItem("com.microsoft.playwright", "driver"));
        index.produce(new IndexDependencyBuildItem("com.microsoft.playwright", "driver-bundle"));
        index.produce(new IndexDependencyBuildItem("com.microsoft.playwright", "playwright"));
    }

    @BuildStep
    NativeImageEnableAllCharsetsBuildItem enableAllCharsetsBuildItem() {
        return new NativeImageEnableAllCharsetsBuildItem();
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //@formatter:off
        final List<String> classNames = new ArrayList<>();

        classNames.add("com.microsoft.playwright.impl.Message");
        classNames.add("com.microsoft.playwright.impl.SerializedArgument");
        classNames.add("com.microsoft.playwright.impl.SerializedValue");
        classNames.add("com.microsoft.playwright.impl.SerializedValue$O");
        classNames.add(Browser.CloseOptions.class.getName());
        classNames.add(Browser.NewContextOptions.class.getName());
        classNames.add(Browser.NewPageOptions.class.getName());
        classNames.add(Browser.StartTracingOptions.class.getName());
        classNames.add(DriverJar.class.getName());
        classNames.add(ElementHandle.CheckOptions.class.getName());
        classNames.add(ElementHandle.ClickOptions.class.getName());
        classNames.add(ElementHandle.DblclickOptions.class.getName());
        classNames.add(ElementHandle.FillOptions.class.getName());
        classNames.add(ElementHandle.HoverOptions.class.getName());
        classNames.add(ElementHandle.InputValueOptions.class.getName());
        classNames.add(ElementHandle.PressOptions.class.getName());
        classNames.add(ElementHandle.ScreenshotOptions.class.getName());
        classNames.add(ElementHandle.ScrollIntoViewIfNeededOptions.class.getName());
        classNames.add(ElementHandle.SelectTextOptions.class.getName());
        classNames.add(ElementHandle.SetCheckedOptions.class.getName());
        classNames.add(ElementHandle.SetInputFilesOptions.class.getName());
        classNames.add(ElementHandle.TapOptions.class.getName());
        classNames.add(ElementHandle.TypeOptions.class.getName());
        classNames.add(ElementHandle.UncheckOptions.class.getName());
        classNames.add(ElementHandle.WaitForElementStateOptions.class.getName());
        classNames.add(ElementHandle.WaitForSelectorOptions.class.getName());
        classNames.add(HttpHeader.class.getName());
        classNames.add(Timing.class.getName());
        classNames.add(ViewportSize.class.getName());
        classNames.add(Frame.AddScriptTagOptions.class.getName());
        classNames.add(Frame.AddStyleTagOptions.class.getName());
        classNames.add(Frame.CheckOptions.class.getName());
        classNames.add(Frame.ClickOptions.class.getName());
        classNames.add(Frame.DblclickOptions.class.getName());
        classNames.add(Frame.DispatchEventOptions.class.getName());
        classNames.add(Frame.FillOptions.class.getName());
        classNames.add(Frame.FocusOptions.class.getName());
        classNames.add(Frame.GetAttributeOptions.class.getName());
        classNames.add(Frame.GetByRoleOptions.class.getName());
        classNames.add(Frame.GetByTextOptions.class.getName());
        classNames.add(Frame.HoverOptions.class.getName());
        classNames.add(Frame.InnerHTMLOptions.class.getName());
        classNames.add(Frame.InnerTextOptions.class.getName());
        classNames.add(Frame.InputValueOptions.class.getName());
        classNames.add(Frame.IsVisibleOptions.class.getName());
        classNames.add(Frame.LocatorOptions.class.getName());
        classNames.add(Frame.PressOptions.class.getName());
        classNames.add(Frame.SelectOptionOptions.class.getName());
        classNames.add(Frame.SetContentOptions.class.getName());
        classNames.add(Frame.SetInputFilesOptions.class.getName());
        classNames.add(Frame.TapOptions.class.getName());
        classNames.add(Frame.TextContentOptions.class.getName());
        classNames.add(Frame.TypeOptions.class.getName());
        classNames.add(Frame.UncheckOptions.class.getName());
        classNames.add(Frame.UncheckOptions.class.getName());
        classNames.add(Frame.WaitForFunctionOptions.class.getName());
        classNames.add(Frame.WaitForLoadStateOptions.class.getName());
        classNames.add(Frame.WaitForSelectorOptions.class.getName());
        classNames.add(Frame.WaitForURLOptions.class.getName());
        classNames.addAll(collectImplementors(combinedIndex, Playwright.class.getName()));

        //@formatter:on
        final TreeSet<String> uniqueClasses = new TreeSet<>(classNames);
        Log.debugf("Playwright Reflection: %s", uniqueClasses);

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(uniqueClasses.toArray(new String[0])).constructors().methods().fields()
                        .serialization().unsafeAllocated().build());
    }

    @BuildStep(onlyIf = IsNormal.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerRuntimeDrivers(PlaywrightRecorder recorder) {
        recorder.initialize();
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    DevServicesResultBuildItem startPlaywrightDevService(
            LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatus,
            PlaywrightBuildTimeConfig config,
            CuratedApplicationShutdownBuildItem shutdown) {
        if (!config.devservices().enabled()) {
            return null;
        }

        if (StringUtils
                .isNotBlank(ConfigProvider.getConfig().getOptionalValue(PLAYWRIGHT_ENDPOINT_CONFIG, String.class).orElse(""))) {
            Log.debugf("Not starting Playwright Dev Services because '%s' is already configured", PLAYWRIGHT_ENDPOINT_CONFIG);
            return null;
        }

        if (!dockerStatus.isContainerRuntimeAvailable()) {
            Log.warn("Docker is not available, Playwright Dev Services will not start");
            return null;
        }

        final PlaywrightDevServiceConfiguration currentConfiguration = new PlaywrightDevServiceConfiguration(
                config.devservices().imageName(),
                config.devservices().verbose(),
                config.devservices().sharedNetwork());

        if (runningDevService != null && Objects.equals(currentConfiguration, capturedDevServiceConfiguration)) {
            return runningDevService.toBuildItem();
        }

        closeRunningDevService();

        if (currentConfiguration.sharedNetwork()) {
            final int httpTestPort = ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.http.test-port", Integer.class)
                    .orElse(8081);
            Testcontainers.exposeHostPorts(httpTestPort);
        }

        final PlaywrightServerContainer container = new PlaywrightServerContainer(currentConfiguration);
        container.start();

        final String endpoint = String.format("ws://%s:%d/", container.getHost(),
                container.getMappedPort(PLAYWRIGHT_SERVER_PORT));
        final Map<String, String> devServiceConfig = Map.of(PLAYWRIGHT_ENDPOINT_CONFIG, endpoint);

        runningDevService = new DevServicesResultBuildItem.RunningDevService(
                FEATURE,
                "Playwright browser server",
                container.getContainerId(),
                container::stop,
                devServiceConfig);
        capturedDevServiceConfiguration = currentConfiguration;

        if (shutdown != null) {
            shutdown.addCloseTask(PlaywrightProcessor::closeRunningDevService, true);
        }
        Log.infof("Playwright Dev Services started at %s using image %s", endpoint, config.devservices().imageName());

        return runningDevService.toBuildItem();
    }

    @BuildStep(onlyIf = IsNormal.class)
    void registerNativeDrivers(BuildProducer<NativeImageResourcePatternsBuildItem> nativeImageResourcePatterns) {
        final NativeImageResourcePatternsBuildItem.Builder builder = NativeImageResourcePatternsBuildItem.builder();
        builder.includeGlob("driver/**");
        nativeImageResourcePatterns.produce(builder.build());
    }

    private List<String> collectSubclasses(CombinedIndexBuildItem combinedIndex, String className) {
        List<String> classes = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(className))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());
        classes.add(className);
        Log.debugf("Subclasses: %s", classes);
        return classes;
    }

    private List<String> collectImplementors(CombinedIndexBuildItem combinedIndex, String className) {
        Set<String> classes = combinedIndex.getIndex()
                .getAllKnownImplementations(DotName.createSimple(className))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toCollection(HashSet::new));
        classes.add(className);
        Set<String> subclasses = new HashSet<>();
        for (String implementationClass : classes) {
            subclasses.addAll(collectSubclasses(combinedIndex, implementationClass));
        }
        classes.addAll(subclasses);
        Log.debugf("Implementors: %s", classes);
        return new ArrayList<>(classes);
    }

    private static synchronized void closeRunningDevService() {
        if (runningDevService != null) {
            try {
                runningDevService.close();
            } catch (IOException e) {
                Log.debug("Failed to stop Playwright Dev Services container", e);
            }
            runningDevService = null;
            capturedDevServiceConfiguration = null;
        }
    }

    private record PlaywrightDevServiceConfiguration(String imageName, boolean verbose, boolean sharedNetwork) {
    }

    static class PlaywrightServerContainer extends GenericContainer<PlaywrightServerContainer> {
        private static final Logger LOGGER = LoggerFactory.getLogger(PlaywrightServerContainer.class);

        private PlaywrightDevServiceConfiguration config;

        PlaywrightServerContainer(PlaywrightDevServiceConfiguration config) {
            super(DockerImageName.parse(config.imageName));
            this.config = config;
            if (config.verbose) {
                withEnv("DEBUG", "pw:api");
            }
            if (config.sharedNetwork) {
                withNetwork(Network.SHARED);
            }
            withExposedPorts(PLAYWRIGHT_SERVER_PORT);
            withCommand("/bin/sh", "-c", PLAYWRIGHT_SERVER_COMMAND);
            waitingFor(Wait.forListeningPort());
        }

        @Override
        public void start() {
            super.start();
            if (config.verbose) {
                followOutput(this::writeToStdOut, OutputFrame.OutputType.STDOUT);
                followOutput(this::writeToStdErr, OutputFrame.OutputType.STDERR);
            }
        }

        private void writeToStdOut(OutputFrame frame) {
            writeOutputFrame(frame, Level.INFO);
        }

        private void writeToStdErr(OutputFrame frame) {
            writeOutputFrame(frame, Level.ERROR);
        }

        private void writeOutputFrame(OutputFrame frame, Level level) {
            LOGGER.atLevel(level).log(frame.getUtf8StringWithoutLineEnding());
        }

    }
}
