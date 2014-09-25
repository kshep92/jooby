package jooby;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jooby.internal.AssetRoute;
import jooby.internal.FallbackBodyConverter;
import jooby.internal.RouteDefinitionImpl;
import jooby.internal.jetty.Jetty;
import jooby.internal.mvc.Routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

/**
 * This is the main entry point for creating a new Jooby application. A Jooby application consist of
 * Modules, Routes and Services.
 *
 * <h1>It is Guice!</h1>
 * <p>
 * Jooby strongly depends on Guice for defining Modules, Routes and Services.
 * </p>
 *
 * <h1>Starting a new application:</h1>
 * <p>
 * A new application must extends Jooby, choose a server implementation, register one ore more
 * {@link BodyConverter} and defines some {@link Router routes}. It sounds like a lot of work to do,
 * but it isn't.
 * </p>
 *
 * <pre>
 * public class MyApp extends Jooby {
 *
 *   {
 *      use(new Jetty());   // 1. server implementation.
 *      use(new Jackson()); // 2. JSON body converter through Jackson.
 *
 *      // 3. Define a route
 *      get("/", (req, res) -> {
 *        Map<String, Object> model = ...;
 *        res.send(model);
 *      }
 *   }
 *
 *  public static void main(String[] args) throws Exception {
 *    new MyApp().start(); // 4. Start it up!
 *  }
 * }
 * </pre>
 *
 * <h1>/application.conf</h1>
 * <p>
 * Jooby delegate configuration management to <a
 * href="https://github.com/typesafehub/config">TypeSafe Config</a>. If you are unfamiliar with <a
 * href="https://github.com/typesafehub/config">TypeSafe Config</a> please takes a few minutes to
 * discover what <a href="https://github.com/typesafehub/config">TypeSafe Config</a> can do for you.
 * </p>
 *
 * <p>
 * By default Jooby looks for an <code>application.conf</code> file at the root of the classpath. If
 * you want to specify a different file or location, you can do it with {@link #use(Config)}.
 * </p>
 *
 * <p>
 * As you already noticed, <a href="https://github.com/typesafehub/config">TypeSafe Config</a> uses
 * a hierarchical model to define and override properties.
 * </p>
 * <p>
 * Each module can also define his own set of properties through {@link JoobyModule#config()}. They
 * will be loaded in the same order the module was registered.
 * </p>
 *
 * <p>
 * In Jooby, system properties takes precedence over any application specific property.
 * </p>
 * <h1>Mode</h1>
 * <p>
 * Jooby defines two modes: <strong>dev</strong> or something else. In Jooby, <strong>dev</strong>
 * is special and some modules applies some special settings while running in <strong>dev</strong>.
 * A none <strong>dev</strong> mode is usually considered a <code>prod</code> like mode. But that
 * depends on module implementor.
 * </p>
 * <p>
 * A mode can be defined in your <code>application.conf</code> file using the
 * <code>application.mode</code> property. If missing, Jooby set the mode for you to
 * <strong>dev</strong>
 * </p>
 * <p>
 * There is more at {@link Mode} so take a few minutes to discover what a {@link Mode} can do for
 * you.
 * </p>
 *
 * <h1>Modules</h1>
 * <p>
 * A module defined by {@link JoobyModule}. It is a super powered Guice module where the configure
 * callback has been complementing with {@link Mode} and {@link Config}.
 * </p>
 *
 * <pre>
 *   public class MyModule implements Module {
 *     public void configure(Mode mode, Config config, Binder binder) {
 *     }
 *   }
 * </pre>
 *
 * From the configure callback you can bind your services as you usually do in a Guice Application.
 * <p>
 * There is more at {@link JoobyModule} so take a few minutes to discover what a {@link JoobyModule}
 * can do for you.
 * </p>
 *
 * <h1>Path Patterns</h1>
 * <p>
 * Jooby supports Ant-style path patterns:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.jsp} or
 * {@code com/txst.html}</li>
 * <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory</li>
 * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath the
 * {@code com} path</li>
 * </ul>
 *
 * <h1>Variable Path Patterns</h1>
 * <p>
 * Jooby supports path parameters too:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/:id</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric
 * <code>id</code> var.</li>
 * </ul>
 *
 * <h1>Routes</h1>
 * <p>
 * Routes are the heart of Jooby! Given an incoming request, Jooby will execute the first route that
 * matches the incoming request path. So, ORDER matters!
 * </p>
 *
 * <h2>Inline route</h2>
 * <p>
 * An inline route can be defined using Lambda expressions, like:
 * </p>
 *
 * <pre>
 *   get("/", (request, response) -> {
 *     response.send("Hello Jooby");
 *   });
 * </pre>
 *
 * Due to the use of lambdas a route is a singleton and you should NOT use shared or global
 * variables. For example this is a bad practice:
 *
 * <pre>
 *  List<String> names = new ArrayList<>(); // names produces side effects
 *  get("/", (request, response) -> {
 *     names.add(request.param("name"));
 *     // response will be different between calls.
 *     response.send(names);
 *   });
 * </pre>
 *
 * <h2>External route</h2>
 * <p>
 * An external route can be defined by using a {@link Class route class}, like:
 * </p>
 *
 * <pre>
 *   get("/", ExternalRoute.class);
 *
 *   ...
 *   // ExternalRoute.java
 *   public class ExternalRoute implements Route {
 *     public void handle(Request request, Response response) throws Exception {
 *       response.send("Hello Jooby");
 *     }
 *   }
 * </pre>
 *
 * <h2>Mvc Route</h2>
 * <p>
 * A Mvc Route use annotations to define routes:
 * </p>
 *
 * <pre>
 *   route(MyRoute.class);
 *   ...
 *   // MyRoute.java
 *   {@literal @}Path("/")
 *   public class MyRoute {
 *
 *    {@literal @}GET
 *    public String hello() {
 *      return "Hello Jooby";
 *    }
 *   }
 * </pre>
 * <p>
 * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
 * simplifications.
 * </p>
 *
 * <p>
 * To learn more about Mvc Routes, please check {@link jooby.mvc.Path}, {@link jooby.mvc.Produces}
 * {@link jooby.mvc.Consumes}, {@link jooby.mvc.Body} and {@link jooby.mvc.Template}.
 * </p>
 *
 * <h1>Assets</h1>
 * <p>
 * An asset is also known as static files, like: *.js, *.css, ..., etc...:
 * </p>
 *
 * <pre>
 *   assets("assets/**");
 * </pre>
 * <p>
 * Here any classpath resource under the <code>/assets</code> folder will be serve it to the client.
 * </p>
 * <h1>Bootstrap</h1>
 * <p>
 * The bootstrap process is defined as follows:
 * </p>
 * <h2>1. Configuration files are loaded in this order:</h2>
 * <ol>
 * <li>System properties</li>
 * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
 * <li>Configuration properties from each of the registered {@link JoobyModule modules}</li>
 * </ol>
 *
 * <h2>2. Dependency Injection and {@link JoobyModule modules}</h2>
 * <ol>
 * <li>An {@link Injector Guice Injector} is created.</li>
 * <li>It configures each of the registered {@link JoobyModule modules}</li>
 * <li>At this point Guice is ready and all the services has been binded.</li>
 * <li>For each registered {@link JoobyModule module} the {@link JoobyModule#start() start method}
 * will be invoked</li>
 * <li>Finally, Jooby ask Guice for a {@link Server web server} and then call to
 * {@link Server#start()} method</li>
 * </ol>
 *
 * @author edgar
 * @since 0.1.0
 * @see JoobyModule
 * @see Request
 * @see Response
 * @see BodyConverter
 * @see Router
 * @see RouteInterceptor
 * @see RequestModule
 */
@Beta
public class Jooby {

  /**
   * Keep track of routes.
   */
  private final Set<Object> routes = new LinkedHashSet<>();

  /**
   * Keep track of modules.
   */
  private final Set<JoobyModule> modules = new LinkedHashSet<>();

  /**
   * Keep track of singleton MVC routes.
   */
  private final Set<Class<?>> singletonRoutes = new LinkedHashSet<>();

  /**
   * Keep track of prototype MVC routes.
   */
  private final Set<Class<?>> protoRoutes = new LinkedHashSet<>();

  /**
   * The override config. Optional.
   */
  private Config config;

  /** The logging system. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /** Keep the global injector instance. */
  private Injector injector;

  {
    use(new Jetty());
  }

  public RouteDefinition use(final Filter filter) {
    return use("/**", filter);
  }

  public RouteDefinition use(final String path, final Filter filter) {
    return route(new RouteDefinitionImpl("*", path, filter));
  }

  /**
   * Define an in-line route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (request, response) -> {
   *     response.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A route path. Required.
   * @param route A route to execute. Required.
   * @return A new route definition.
   */
  public RouteDefinition get(final String path, final Router route) {
    return route(new RouteDefinitionImpl("GET", path, route));
  }

  public RouteDefinition get(final String path, final Filter filter) {
    return route(new RouteDefinitionImpl("GET", path, filter));
  }

  /**
   * Define an in-line route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (request, response) -> {
   *     response.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A route path. Required.
   * @param route A route to execute. Required.
   * @return A new route definition.
   */
  public RouteDefinition post(final String path, final Router route) {
    return route(new RouteDefinitionImpl("POST", path, route));
  }

  public RouteDefinition post(final String path, final Filter filter) {
    return route(new RouteDefinitionImpl("POST", path, filter));
  }

  /**
   * Define an in-line route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (request, response) -> {
   *     response.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A route path. Required.
   * @param route A route to execute. Required.
   * @return A new route definition.
   */
  public RouteDefinition put(final String path, final Router route) {
    return route(new RouteDefinitionImpl("PUT", path, route));
  }

  public RouteDefinition put(final String path, final Filter filter) {
    return route(new RouteDefinitionImpl("PUT", path, filter));
  }

  /**
   * Define an in-line route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (request, response) -> {
   *     response.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A route path. Required.
   * @param router A route to execute. Required.
   * @return A new route definition.
   */
  public RouteDefinition delete(final String path, final Router router) {
    return route(new RouteDefinitionImpl("DELETE", path, router));
  }

  public RouteDefinition delete(final String path, final Filter filter) {
    return route(new RouteDefinitionImpl("DELETE", path, filter));
  }

  /**
   * Convert an external route to an inline route.
   *
   * @param route The external route class.
   * @return A new inline route.
   */
  private static Router wrapRouter(final Class<? extends Router> route) {
    return (req, resp) -> req.getInstance(route).handle(req, resp);
  }

  /**
   * Publish static files to the client. This method is useful for serving javascript, css and any
   * other static file.
   *
   * <pre>
   *   assets("/assets/**");
   * </pre>
   *
   * It publish the content of <code>/assets/**</code> classpath folder.
   *
   * @param path The path to publish. Required.
   * @return A new route definition.
   */
  public RouteDefinition assets(final String path) {
    return get(path, wrapRouter(AssetRoute.class));
  }

  /**
   * <p>
   * A Mvc Route use annotations to define routes:
   * </p>
   *
   * <pre>
   *   route(MyRoute.class);
   *   ...
   *   // MyRoute.java
   *   {@literal @}Path("/")
   *   public class MyRoute {
   *
   *    {@literal @}GET
   *    public String hello() {
   *      return "Hello Jooby";
   *    }
   *   }
   * </pre>
   * <p>
   * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
   * simplifications.
   * </p>
   *
   * <p>
   * A new instance is created per request, not like inline routes. So an Mvc route isn't singleton.
   * This scope is known us prototype or per-lookup.
   * </p>
   * <p>
   * To learn more about Mvc Routes, please check {@link jooby.mvc.Path}, {@link jooby.mvc.Produces}
   * {@link jooby.mvc.Consumes}, {@link jooby.mvc.Body} and {@link jooby.mvc.Template}.
   * </p>
   *
   * @param routeType The Mvc route.
   */
  public void route(final Class<?> routeType) {
    requireNonNull(routeType, "Route type is required.");
    if (routeType.getAnnotation(javax.inject.Singleton.class) == null) {
      protoRoutes.add(routeType);
    } else {
      singletonRoutes.add(routeType);
    }
    routes.add(routeType);
  }

  /**
   * Keep track of routes in the order user define them.
   *
   * @param route A route definition to append.
   * @return The same route definition.
   */
  private RouteDefinition route(final RouteDefinition route) {
    routes.add(route);
    return route;
  }

  /**
   * Register a Jooby module. Module are executed in the order they were registered.
   *
   * @param module The module to register. Required.
   * @return This Jooby instance.
   * @see JoobyModule
   */
  public Jooby use(final JoobyModule module) {
    requireNonNull(module, "A module is required.");
    modules.add(module);
    return this;
  }

  /**
   * Set the application configuration object. You must call this method when the default file
   * name: <code>application.conf</code> doesn't work for you or when you need/want to register two
   * or more files.
   *
   * @param config The application configuration object. Required.
   * @return This Jooby instance.
   * @see Config
   */
  public Jooby use(final Config config) {
    this.config = requireNonNull(config, "A config is required.");
    return this;
  }

  /**
   * <h1>Bootstrap</h1>
   * <p>
   * The bootstrap process is defined as follows:
   * </p>
   * <h2>1. Configuration files order and fall-backs</h2>
   * <ol>
   * <li>System properties are loaded</li>
   * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
   * <li>Load configuration properties from each of the registered {@link JoobyModule modules}</li>
   * <li>At this point a {@link Config} object is ready to use</li>
   * </ol>
   *
   * <h2>2. Dependency Injection and {@link JoobyModule modules}</h2>
   * <ol>
   * <li>An {@link Injector Guice Injector} is created.</li>
   * <li>It configures each of the registered {@link JoobyModule modules}</li>
   * <li>At this point Guice is ready and all the services has been binded.</li>
   * <li>For each registered {@link JoobyModule module} the {@link JoobyModule#start() start method}
   * will be invoked</li>
   * <li>Finally, Jooby ask Guice for a {@link Server web server} and then call to
   * {@link Server#start()}</li>
   * </ol>
   *
   * @throws Exception If something fails to start.
   */
  public void start() throws Exception {
    config = buildConfig(Optional.ofNullable(config));

    // shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        stop();
      } catch (Exception ex) {
        log.error("Shutdown with error", ex);
      }
    }));

    final Charset charset = Charset.forName(config.getString("application.charset"));

    // dependency injection
    injector = Guice.createInjector(new Module() {
      @Override
      public void configure(final Binder binder) {
        // bind config
        bindConfig(binder, config);

        // bind mode
        Mode mode = mode(config.getString("application.mode").toLowerCase());
        binder.bind(Mode.class).toInstance(mode);

        // bind charset
        binder.bind(Charset.class).toInstance(charset);

        // bind readers & writers
        Multibinder<BodyConverter> converters = Multibinder
            .newSetBinder(binder, BodyConverter.class);

        // Routes
        Multibinder<RouteDefinition> definitions = Multibinder
            .newSetBinder(binder, RouteDefinition.class);

        // Request Modules
        Multibinder<RequestModule> requestModule = Multibinder
            .newSetBinder(binder, RequestModule.class);

        // bind prototype routes in request module
        requestModule.addBinding().toInstance(
            rm -> protoRoutes.forEach(routeClass -> rm.bind(routeClass)));

        // tmp dir
        binder.bind(File.class).annotatedWith(Names.named("java.io.tmpdir"))
            .toInstance(new File(config.getString("java.io.tmpdir")));

        // configure module
        modules.forEach(m -> {
          install(m, mode, config, binder);
        });

        // Routes
        routes.forEach(candidate -> {
          if (candidate instanceof RouteDefinition) {
            definitions.addBinding().toInstance((RouteDefinitionImpl) candidate);
          } else {
            Class<?> routeClass = (Class<?>) candidate;
            Routes.routes(mode, routeClass)
                .forEach(route -> definitions.addBinding().toInstance(route));
          }
        });

        // Singleton routes
        singletonRoutes.forEach(routeClass -> binder.bind(routeClass).in(Scopes.SINGLETON));

        converters.addBinding().toInstance(FallbackBodyConverter.COPY_TEXT);
        converters.addBinding().toInstance(FallbackBodyConverter.COPY_BYTES);
        converters.addBinding().toInstance(FallbackBodyConverter.READ_TEXT);
        converters.addBinding().toInstance(FallbackBodyConverter.TO_HTML);
      }

    });

    // start modules
    for (JoobyModule module : modules) {
      module.start();
    }

    // Start server
    Server server = injector.getInstance(Server.class);

    server.start();
  }

  /**
   * Stop the application, close all the modules and stop the web server.
   */
  public void stop() throws Exception {
    // stop modules
    for (JoobyModule module : modules) {
      try {
        module.stop();
      } catch (Exception ex) {
        log.error("Can't stop: " + module.getClass().getName(), ex);
      }
    }

    try {
      if (injector != null) {
        Server server = injector.getInstance(Server.class);
        server.stop();
      }
    } catch (Exception ex) {
      log.error("Can't stop server", ex);
    }
  }

  /**
   * Build configuration properties, it configure system, app and modules properties.
   *
   * @param appConfig An optional app configuration.
   * @return A configuration properties ready to use.
   */
  private Config buildConfig(final Optional<Config> appConfig) {
    Config sysProps = ConfigFactory.defaultOverrides()
        // file encoding got corrupted sometimes so we force and override.
        .withValue("file.encoding",
            ConfigValueFactory.fromAnyRef(System.getProperty("file.encoding")));

    // app configuration
    Supplier<Config> defaults = () -> ConfigFactory.parseResources("application.conf");
    Config config = sysProps
        .withFallback(appConfig.orElseGet(defaults));

    // set app name
    if (!config.hasPath("application.name")) {
      config = config.withValue("application.name",
          ConfigValueFactory.fromAnyRef(getClass().getSimpleName()));
    }

    // set default charset, if app config didn't set it
    if (!config.hasPath("application.charset")) {
      config = config.withValue("application.charset",
          ConfigValueFactory.fromAnyRef(Charset.defaultCharset().name()));
    }

    // set module config
    for (JoobyModule module : ImmutableList.copyOf(modules).reverse()) {
      config = config.withFallback(module.config());
    }

    // add default config + mime types
    config = config
        .withFallback(ConfigFactory.parseResources("jooby/mime.properties"));
    config = config
        .withFallback(ConfigFactory.parseResources("jooby/jooby.conf"));

    return config.resolve();
  }

  /**
   * Install a {@link JoobyModule}.
   *
   * @param module The module to install.
   * @param mode Application mode.
   * @param config The configuration object.
   * @param binder A Guice binder.
   */
  private void install(final JoobyModule module, final Mode mode, final Config config,
      final Binder binder) {
    try {
      module.configure(mode, config, binder);
    } catch (Exception ex) {
      throw new IllegalStateException("Module didn't start properly: "
          + module.getClass().getName(), ex);
    }
  }

  /**
   * Bind a {@link Config} and make it available for injection. Each property of the config is also
   * binded it and ready to be injected with {@link javax.inject.Named}.
   *
   * @param binder
   * @param config
   */
  @SuppressWarnings("unchecked")
  private void bindConfig(final Binder root, final Config config) {
    Binder binder = root.skipSources(Names.class);
    for (Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      Named named = Names.named(name);
      Object value = entry.getValue().unwrapped();
      if (value instanceof List) {
        List<Object> values = (List<Object>) value;
        Type listType = Types.listOf(values.iterator().next().getClass());
        Key<Object> key = (Key<Object>) Key.get(listType, Names.named(name));
        binder.bind(key).toInstance(values);
      } else {
        @SuppressWarnings("rawtypes")
        Class type = value.getClass();
        binder.bind(type).annotatedWith(named).toInstance(value);
      }
    }
    // bind config
    binder.bind(Config.class).toInstance(config);
  }

  /**
   * Creates the application's mode.
   *
   * @param name A mode's name.
   * @return A new mode.
   */
  private static Mode mode(final String name) {
    return new Mode() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }
}