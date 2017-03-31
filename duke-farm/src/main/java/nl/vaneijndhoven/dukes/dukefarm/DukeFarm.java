package nl.vaneijndhoven.dukes.dukefarm;

import com.hazelcast.config.Config;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import nl.vaneijndhoven.daisy.Daisy;
import nl.vaneijndhoven.dukes.bo.Bo;
import nl.vaneijndhoven.dukes.cooter.Command;
import nl.vaneijndhoven.dukes.cooter.Engine;
import nl.vaneijndhoven.dukes.cooter.Steering;
import nl.vaneijndhoven.dukes.cooter.car.Car;
import nl.vaneijndhoven.dukes.flash.Flash;
import nl.vaneijndhoven.dukes.generallee.EngineMap;
import nl.vaneijndhoven.dukes.generallee.SteeringMap;
import nl.vaneijndhoven.dukes.hazardcounty.Environment;
import nl.vaneijndhoven.dukes.luke.Luke;
import nl.vaneijndhoven.dukes.unclejesse.UncleJesse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.vaneijndhoven.dukes.hazardcounty.Environment.RASPBERRY_PI_IP;

public class DukeFarm {

    private static final Logger LOG = LoggerFactory.getLogger(DukeFarm.class);

    public static void main(String... args) throws Exception {
        Car car = new Car(new Engine(new EngineMap()), new Steering(new SteeringMap()));

        configureLogging();
        configureShutdownHook(car);

        // Set wheel and speed to neutral.
        car.stop();
//        Command.stop();

        LOG.info("Firing up General Lee vert.x and core controller, tutu tu tu tu tutututu tututu...");

        VertxOptions options = new VertxOptions()
                .setClustered(true)
                .setClusterManager(createHazelcastConfig());

        if (Environment.getInstance().runningOnRaspberryPi()) {
            LOG.info("Running on the Raspberry Pi, activating Vert.x clustering over the network.");
            // activate clustering over the network on the right interface.
            options.setClusterHost(RASPBERRY_PI_IP);
        } else {
            LOG.info("Not running on the Raspberry Pi, not activating Vert.x clustering over the network.");
        }

        Vertx.clusteredVertx(options, resultHandler -> {
            Vertx vertx = resultHandler.result();
            DeploymentOptions deploymentOptions = new DeploymentOptions();
            deploymentOptions.setWorker(true);
            vertx.deployVerticle(new Flash(), deploymentOptions);
            vertx.deployVerticle(new Bo(), deploymentOptions);
            vertx.deployVerticle(new Daisy(), deploymentOptions);
            vertx.deployVerticle(new Luke(), deploymentOptions);
            vertx.deployVerticle(new UncleJesse(), deploymentOptions);
        });
    }

    public static void configureShutdownHook(Car car) {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                LOG.info("Activating shutdown hook.");
                car.stop();
                Command.statusLedOff();
            }
        });
    }

    public static void configureLogging() {
        System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());

//        LoggerContext logConfig = (LoggerContext) LoggerFactory.getILoggerFactory();
//        logConfig.getLogger("io.vertx").setLevel(Level.INFO);
//        logConfig.getLogger("com.hazelcast").setLevel(Level.ERROR);
//        logConfig.getLogger("io.netty").setLevel(Level.ERROR);
//
//        logConfig.getLogger("ROOT").setLevel(Level.INFO);
//        logConfig.getLogger("nl.revolution.dukes").setLevel(Level.DEBUG);

    }


    public static ClusterManager createHazelcastConfig() {
        Config hazelcastConfig = new Config();
        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);
        return mgr;

    }

}
